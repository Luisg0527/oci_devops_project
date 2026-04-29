package com.ociproject.service.ai;

import com.ociproject.model.ProjectDocEmbedding.SourceType;

public record ScoredDoc(SourceType sourceType, Long sourceId, String content, double score) { }
