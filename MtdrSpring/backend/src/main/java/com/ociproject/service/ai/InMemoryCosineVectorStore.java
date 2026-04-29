package com.ociproject.service.ai;

import com.ociproject.model.ProjectDocEmbedding;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.repository.ProjectDocEmbeddingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
@RequiredArgsConstructor
public class InMemoryCosineVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCosineVectorStore.class);

    private final ProjectDocEmbeddingRepository repository;

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private volatile LocalDateTime lastLoadedAt = null;

    private record Entry(SourceType type, Long sourceId, String content, float[] vector) {}

    @PostConstruct
    public void init() {
        reload();
    }

    private synchronized void reload() {
        cache.clear();
        for (ProjectDocEmbedding e : repository.findAll()) {
            float[] vec = EmbeddingUtil.bytesToFloats(e.getEmbedding());
            cache.put(key(e.getSourceType(), e.getSourceId()),
                    new Entry(e.getSourceType(), e.getSourceId(), e.getContent(), vec));
        }
        lastLoadedAt = repository.findMaxUpdatedAt().orElse(null);
        log.info("VectorStore reloaded: {} entries", cache.size());
    }

    @Override
    public void reloadIfStale() {
        LocalDateTime dbMax = repository.findMaxUpdatedAt().orElse(null);
        if (dbMax == null) return;
        if (lastLoadedAt == null || dbMax.isAfter(lastLoadedAt)) {
            reload();
        }
    }

    @Override
    @Transactional
    public void upsert(SourceType type, Long sourceId, String content, float[] embedding) {
        ProjectDocEmbedding existing = repository.findBySourceTypeAndSourceId(type, sourceId).orElse(null);
        ProjectDocEmbedding entity;
        if (existing != null) {
            existing.setContent(content);
            existing.setEmbedding(EmbeddingUtil.floatsToBytes(embedding));
            existing.setEmbedDim(embedding.length);
            entity = existing;
        } else {
            entity = ProjectDocEmbedding.builder()
                    .sourceType(type)
                    .sourceId(sourceId)
                    .content(content)
                    .embedding(EmbeddingUtil.floatsToBytes(embedding))
                    .embedDim(embedding.length)
                    .build();
        }
        repository.save(entity);
        cache.put(key(type, sourceId), new Entry(type, sourceId, content, embedding));
        lastLoadedAt = LocalDateTime.now();
    }

    @Override
    public List<ScoredDoc> topK(float[] queryEmbedding, int k, Set<SourceType> filter) {
        reloadIfStale();
        PriorityQueue<ScoredDoc> heap = new PriorityQueue<>(Comparator.comparingDouble(ScoredDoc::score));
        for (Entry e : cache.values()) {
            if (filter != null && !filter.isEmpty() && !filter.contains(e.type())) continue;
            double score = EmbeddingUtil.cosine(queryEmbedding, e.vector());
            ScoredDoc doc = new ScoredDoc(e.type(), e.sourceId(), e.content(), score);
            if (heap.size() < k) {
                heap.offer(doc);
            } else if (heap.peek() != null && score > heap.peek().score()) {
                heap.poll();
                heap.offer(doc);
            }
        }
        List<ScoredDoc> result = new ArrayList<>(heap);
        result.sort((a, b) -> Double.compare(b.score(), a.score()));
        return result;
    }

    @Override
    @Transactional
    public void deleteByType(SourceType type) {
        repository.deleteBySourceType(type);
        cache.entrySet().removeIf(e -> e.getValue().type() == type);
    }

    @Override
    public int count() {
        return cache.size();
    }

    private static String key(SourceType type, Long sourceId) {
        return type.name() + "#" + sourceId;
    }
}
