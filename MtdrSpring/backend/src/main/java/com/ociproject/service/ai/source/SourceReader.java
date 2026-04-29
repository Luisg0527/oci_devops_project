package com.ociproject.service.ai.source;

import com.ociproject.model.ProjectDocEmbedding.SourceType;

import java.util.List;

public interface SourceReader {

    SourceType type();

    List<DocumentChunk> readAll();
}
