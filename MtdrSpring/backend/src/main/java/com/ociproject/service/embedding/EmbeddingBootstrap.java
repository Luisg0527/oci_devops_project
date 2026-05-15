package com.ociproject.service.embedding;

import com.ociproject.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * On application startup, ensures the Qdrant collection exists and — if it is
 * empty — performs an initial bulk index of every embeddable entity from
 * Oracle. The work runs asynchronously so app boot is never blocked.
 *
 * Safe to run on every replica: the upsert is idempotent (deterministic point
 * IDs) and only triggers when the collection is empty, so concurrent bootstraps
 * across replicas converge to the same final state.
 */
@Component
@RequiredArgsConstructor
public class EmbeddingBootstrap {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBootstrap.class);

    private final RagProperties ragProps;
    private final EmbeddingClient embeddingClient;
    private final QdrantVectorStore vectorStore;
    private final EmbeddingService embeddingService;

    @Async("embeddingExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!ragProps.isEnabled()) {
            log.info("RAG disabled (rag.enabled=false). Skipping vector bootstrap.");
            return;
        }
        if (!embeddingClient.isConfigured()) {
            log.warn("Embedding client not configured (GEMINI_API_KEY missing). Skipping bootstrap; "
                    + "/ai/chat will fall back to the full snapshot.");
            return;
        }
        if (!vectorStore.isReachable()) {
            log.warn("Qdrant not reachable. Skipping bootstrap; /ai/chat will fall back.");
            return;
        }

        try {
            vectorStore.ensureCollection(embeddingClient.dimensions());
        } catch (Exception e) {
            log.warn("Could not ensure Qdrant collection: {}", e.getMessage());
            return;
        }

        if (!ragProps.isBootstrapOnStartup()) {
            log.info("rag.bootstrap-on-startup=false — skipping bulk re-index.");
            return;
        }

        long existing = vectorStore.count();
        if (existing > 0) {
            log.info("Qdrant collection already has {} points. Skipping bulk re-index.", existing);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            EmbeddingService.BulkResult result = embeddingService.reindexAll();
            log.info("Bootstrap done: indexed {} points in {} ms.",
                    result.total, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Bootstrap re-index failed: {}", e.getMessage(), e);
        }
    }
}
