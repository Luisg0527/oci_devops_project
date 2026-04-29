package com.ociproject.service.ai;

import com.ociproject.dto.response.IngestionReport;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.service.ai.source.DocumentChunk;
import com.ociproject.service.ai.source.SourceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final List<SourceReader> readers;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public IngestionService(List<SourceReader> readers, EmbeddingService embeddingService, VectorStore vectorStore) {
        this.readers = readers;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public IngestionReport reindexAll() {
        long t0 = System.currentTimeMillis();
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (SourceReader r : readers) {
            try {
                int n = ingestOne(r);
                counts.put(r.type().name(), n);
            } catch (Exception e) {
                log.error("Reindex failed for {}", r.type(), e);
                errors.add(r.type().name() + ": " + e.getMessage());
            }
        }
        return IngestionReport.builder()
                .upserted(counts)
                .elapsedMs(System.currentTimeMillis() - t0)
                .errors(errors)
                .build();
    }

    public IngestionReport reindex(SourceType type) {
        long t0 = System.currentTimeMillis();
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        SourceReader r = readers.stream()
                .filter(x -> x.type() == type)
                .findFirst()
                .orElse(null);
        if (r == null) {
            errors.add("No reader registered for " + type);
        } else {
            try {
                counts.put(type.name(), ingestOne(r));
            } catch (Exception e) {
                log.error("Reindex failed for {}", type, e);
                errors.add(type.name() + ": " + e.getMessage());
            }
        }
        return IngestionReport.builder()
                .upserted(counts)
                .elapsedMs(System.currentTimeMillis() - t0)
                .errors(errors)
                .build();
    }

    private int ingestOne(SourceReader reader) {
        List<DocumentChunk> chunks = reader.readAll();
        if (chunks.isEmpty()) return 0;
        List<String> texts = chunks.stream().map(DocumentChunk::content).toList();
        List<float[]> vectors = embeddingService.embedBatch(texts, EmbeddingService.InputType.SEARCH_DOCUMENT);
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count mismatch for " + reader.type()
                    + ": " + vectors.size() + " vs " + chunks.size());
        }
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk c = chunks.get(i);
            vectorStore.upsert(c.sourceType(), c.sourceId(), c.content(), vectors.get(i));
        }
        return chunks.size();
    }
}
