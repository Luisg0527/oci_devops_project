package com.ociproject.service.embedding;

import com.ociproject.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges Hibernate flush events (published as {@link EmbeddingChangeEvent})
 * with the actual Qdrant upsert/delete work. The work runs:
 * - AFTER the surrounding transaction commits (consistency)
 * - on a dedicated thread pool (latency isolation)
 * - swallowing exceptions (a failed embedding must never break user flow)
 */
@Component
@RequiredArgsConstructor
public class EmbeddingEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingEventDispatcher.class);

    private final EmbeddingService embeddingService;
    private final RagProperties ragProps;

    @Async("embeddingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChange(EmbeddingChangeEvent event) {
        if (!ragProps.isEnabled()) return;
        try {
            embeddingService.handle(event);
        } catch (Exception e) {
            log.warn("Embedding {} for {}/{} failed: {}",
                    event.getKind(), event.getSourceType(), event.getSourceId(), e.getMessage());
        }
    }
}
