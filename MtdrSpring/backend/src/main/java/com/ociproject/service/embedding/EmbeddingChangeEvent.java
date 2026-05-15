package com.ociproject.service.embedding;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Domain event published whenever an embeddable entity is inserted, updated,
 * or deleted. The actual Qdrant upsert/delete runs asynchronously after the
 * surrounding transaction commits.
 */
@Getter
@RequiredArgsConstructor
public class EmbeddingChangeEvent {

    public enum Kind { UPSERT, DELETE }

    private final SourceType sourceType;
    private final Long sourceId;
    private final Kind kind;

    public static EmbeddingChangeEvent upsert(SourceType type, Long id) {
        return new EmbeddingChangeEvent(type, id, Kind.UPSERT);
    }

    public static EmbeddingChangeEvent delete(SourceType type, Long id) {
        return new EmbeddingChangeEvent(type, id, Kind.DELETE);
    }
}
