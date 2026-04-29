package com.ociproject.service.ai;

import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.service.ai.source.DocumentChunk;
import com.ociproject.service.ai.source.SourceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ContextDumperService {

    private static final Logger log = LoggerFactory.getLogger(ContextDumperService.class);

    private static final List<SourceType> ORDER = List.of(
            SourceType.PROJECT,
            SourceType.SPRINT,
            SourceType.TASK,
            SourceType.USER,
            SourceType.KPI_VALUE,
            SourceType.DEPLOYMENT,
            SourceType.INCIDENT
    );

    private final Map<SourceType, SourceReader> readersByType;

    public ContextDumperService(List<SourceReader> readers) {
        Map<SourceType, SourceReader> map = new EnumMap<>(SourceType.class);
        for (SourceReader r : readers) {
            map.put(r.type(), r);
        }
        this.readersByType = map;
    }

    public DumpResult dumpAll(int maxChars) {
        StringBuilder sb = new StringBuilder();
        List<DocumentRef> included = new ArrayList<>();
        int truncatedCount = 0;

        for (SourceType type : ORDER) {
            SourceReader reader = readersByType.get(type);
            if (reader == null) continue;
            List<DocumentChunk> chunks;
            try {
                chunks = reader.readAll();
            } catch (Exception e) {
                log.warn("SourceReader {} failed: {}", type, e.getMessage());
                continue;
            }
            for (DocumentChunk chunk : chunks) {
                String header = "[" + chunk.sourceType().name() + "#" + chunk.sourceId() + "]\n";
                String entry = header + chunk.content().trim() + "\n---\n";
                if (sb.length() + entry.length() > maxChars) {
                    truncatedCount++;
                    continue;
                }
                sb.append(entry);
                included.add(new DocumentRef(chunk.sourceType(), chunk.sourceId()));
            }
        }
        if (truncatedCount > 0) {
            log.info("Context dump: {} docs included, {} truncated, {} chars used (max {})",
                    included.size(), truncatedCount, sb.length(), maxChars);
        } else {
            log.debug("Context dump: {} docs included, {} chars used", included.size(), sb.length());
        }
        included.sort(Comparator.comparing((DocumentRef r) -> r.sourceType().name())
                .thenComparingLong(DocumentRef::sourceId));
        return new DumpResult(sb.toString(), included);
    }

    public record DocumentRef(SourceType sourceType, Long sourceId) {}

    public record DumpResult(String contextBlock, List<DocumentRef> included) {}
}
