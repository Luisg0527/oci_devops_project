package com.ociproject.service.ai.source;

import com.ociproject.model.ProjectDocEmbedding.SourceType;

public record DocumentChunk(SourceType sourceType, Long sourceId, String content) { }
