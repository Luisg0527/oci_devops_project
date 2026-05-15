package com.ociproject.service.embedding;

import com.ociproject.model.*;
import com.ociproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Translates {@link EmbeddingChangeEvent}s and bulk re-index requests into
 * canonical text → embedding → Qdrant upsert operations.
 *
 * Soft-deletes (entity.deleted = true) are folded into DELETE so the vector
 * index stays consistent with what the user actually sees.
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int BULK_BATCH = 100;

    private final EmbeddingClient embeddingClient;
    private final QdrantVectorStore vectorStore;
    private final CanonicalTextBuilder textBuilder;

    private final ProjectRepository projectRepo;
    private final SprintRepository sprintRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final TeamRepository teamRepo;
    private final KpiValueRepository kpiValueRepo;
    private final DeploymentRepository deploymentRepo;
    private final IncidentRepository incidentRepo;

    /**
     * Handle a single change event. Idempotent: missing rows → delete,
     * soft-deleted rows → delete, present rows → upsert.
     */
    @Transactional(readOnly = true)
    public void handle(EmbeddingChangeEvent event) {
        SourceType type = event.getSourceType();
        Long id = event.getSourceId();
        if (id == null) return;

        if (event.getKind() == EmbeddingChangeEvent.Kind.DELETE) {
            vectorStore.delete(type, id);
            return;
        }

        Optional<Indexable> doc = loadIndexable(type, id);
        if (doc.isEmpty()) {
            vectorStore.delete(type, id);
            return;
        }

        Indexable d = doc.get();
        float[] vec = embeddingClient.embed(d.text());
        vectorStore.upsert(type, id, vec, d.payload());
    }

    /** Bulk index every embeddable entity. Used by {@code EmbeddingBootstrap}. */
    @Transactional(readOnly = true)
    public BulkResult reindexAll() {
        BulkResult result = new BulkResult();
        result.add(indexAll(SourceType.PROJECT, projectRepo.findAll(),
                p -> ((Project) p).getProjectId(),
                p -> Boolean.TRUE.equals(((Project) p).getDeleted()),
                p -> textBuilder.text((Project) p),
                p -> textBuilder.payload((Project) p)));
        result.add(indexAll(SourceType.SPRINT, sprintRepo.findAll(),
                s -> ((Sprint) s).getSprintId(),
                s -> Boolean.TRUE.equals(((Sprint) s).getDeleted()),
                s -> textBuilder.text((Sprint) s),
                s -> textBuilder.payload((Sprint) s)));
        result.add(indexAll(SourceType.TASK, taskRepo.findAll(),
                t -> ((Task) t).getTaskId(),
                t -> Boolean.TRUE.equals(((Task) t).getDeleted()),
                t -> textBuilder.text((Task) t),
                t -> textBuilder.payload((Task) t)));
        result.add(indexAll(SourceType.USER, userRepo.findAll(),
                u -> ((User) u).getUserId(),
                u -> Boolean.TRUE.equals(((User) u).getDeleted()),
                u -> textBuilder.text((User) u),
                u -> textBuilder.payload((User) u)));
        result.add(indexAll(SourceType.TEAM, teamRepo.findAll(),
                t -> ((Team) t).getTeamId(),
                t -> Boolean.TRUE.equals(((Team) t).getDeleted()),
                t -> textBuilder.text((Team) t),
                t -> textBuilder.payload((Team) t)));
        result.add(indexAll(SourceType.KPI_VALUE, kpiValueRepo.findAll(),
                k -> ((KpiValue) k).getKpiValueId(),
                k -> false,
                k -> textBuilder.text((KpiValue) k),
                k -> textBuilder.payload((KpiValue) k)));
        result.add(indexAll(SourceType.DEPLOYMENT, deploymentRepo.findAll(),
                d -> ((Deployment) d).getDeploymentId(),
                d -> false,
                d -> textBuilder.text((Deployment) d),
                d -> textBuilder.payload((Deployment) d)));
        result.add(indexAll(SourceType.INCIDENT, incidentRepo.findAll(),
                i -> ((Incident) i).getIncidentId(),
                i -> Boolean.TRUE.equals(((Incident) i).getDeleted()),
                i -> textBuilder.text((Incident) i),
                i -> textBuilder.payload((Incident) i)));
        return result;
    }

    @SuppressWarnings("unchecked")
    private <E> int indexAll(SourceType type,
                              List<E> all,
                              java.util.function.Function<Object, Long> idOf,
                              java.util.function.Function<Object, Boolean> isDeleted,
                              java.util.function.Function<Object, String> toText,
                              java.util.function.Function<Object, Map<String, Object>> toPayload) {
        List<E> active = new ArrayList<>();
        for (E e : all) {
            if (Boolean.TRUE.equals(isDeleted.apply(e))) continue;
            active.add(e);
        }
        if (active.isEmpty()) {
            log.info("Bootstrap: no {} rows to index.", type);
            return 0;
        }

        int indexed = 0;
        for (int i = 0; i < active.size(); i += BULK_BATCH) {
            List<E> chunk = active.subList(i, Math.min(i + BULK_BATCH, active.size()));
            List<String> texts = new ArrayList<>(chunk.size());
            for (E e : chunk) texts.add(toText.apply(e));

            List<float[]> vectors = embeddingClient.embedBatch(texts);
            List<QdrantVectorStore.UpsertItem> items = new ArrayList<>(chunk.size());
            for (int j = 0; j < chunk.size(); j++) {
                E e = chunk.get(j);
                items.add(new QdrantVectorStore.UpsertItem(
                        type, idOf.apply(e), vectors.get(j), toPayload.apply(e)));
            }
            vectorStore.upsertBatch(items);
            indexed += chunk.size();
            log.info("Bootstrap: indexed {}/{} {} rows.", indexed, active.size(), type);
        }
        return indexed;
    }

    /**
     * Hydrate the search hits back into full entities so the LLM gets real
     * data, not stale payload snapshots. Returns canonical text for each hit,
     * preserving the search-score order. Missing/deleted rows are skipped.
     */
    @Transactional(readOnly = true)
    public List<HydratedDoc> hydrate(List<QdrantVectorStore.Hit> hits) {
        List<HydratedDoc> out = new ArrayList<>();
        for (QdrantVectorStore.Hit hit : hits) {
            SourceType type = hit.sourceType();
            Long id = hit.sourceId();
            if (type == null || id == null) continue;
            Optional<Indexable> doc = loadIndexable(type, id);
            doc.ifPresent(d -> out.add(new HydratedDoc(type, id, hit.getScore(), d.text())));
        }
        return out;
    }

    // ── internal ──

    private Optional<Indexable> loadIndexable(SourceType type, Long id) {
        switch (type) {
            case PROJECT:
                return projectRepo.findById(id)
                        .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                        .map(p -> new Indexable(textBuilder.text(p), textBuilder.payload(p)));
            case SPRINT:
                return sprintRepo.findById(id)
                        .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                        .map(s -> new Indexable(textBuilder.text(s), textBuilder.payload(s)));
            case TASK:
                return taskRepo.findById(id)
                        .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                        .map(t -> new Indexable(textBuilder.text(t), textBuilder.payload(t)));
            case USER:
                return userRepo.findById(id)
                        .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                        .map(u -> new Indexable(textBuilder.text(u), textBuilder.payload(u)));
            case TEAM:
                return teamRepo.findById(id)
                        .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                        .map(t -> new Indexable(textBuilder.text(t), textBuilder.payload(t)));
            case KPI_VALUE:
                return kpiValueRepo.findById(id)
                        .map(k -> new Indexable(textBuilder.text(k), textBuilder.payload(k)));
            case DEPLOYMENT:
                return deploymentRepo.findById(id)
                        .map(d -> new Indexable(textBuilder.text(d), textBuilder.payload(d)));
            case INCIDENT:
                return incidentRepo.findById(id)
                        .filter(i -> !Boolean.TRUE.equals(i.getDeleted()))
                        .map(i -> new Indexable(textBuilder.text(i), textBuilder.payload(i)));
            default:
                return Optional.empty();
        }
    }

    public record Indexable(String text, Map<String, Object> payload) {}

    public record HydratedDoc(SourceType type, Long sourceId, Double score, String text) {}

    public static class BulkResult {
        public int total;
        public void add(int n) { total += n; }
    }
}
