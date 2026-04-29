package com.ociproject.service.ai;

import com.ociproject.model.ProjectDocEmbedding.SourceType;

import java.util.List;
import java.util.Set;

public interface VectorStore {

    void upsert(SourceType type, Long sourceId, String content, float[] embedding);

    List<ScoredDoc> topK(float[] queryEmbedding, int k, Set<SourceType> filter);

    void deleteByType(SourceType type);

    int count();

    void reloadIfStale();
}
