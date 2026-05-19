package com.ociproject.service.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ociproject.config.QdrantProperties;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

/**
 * Thin wrapper around the Qdrant REST API ({@code :6333}). Keeps the rest of
 * the codebase free of HTTP/JSON wiring. All operations are idempotent so they
 * are safe to retry from event handlers.
 *
 * Point IDs are deterministic UUIDs derived from {@code (sourceType, sourceId)}
 * so the same logical document always maps to the same Qdrant point.
 */
@Service
@RequiredArgsConstructor
public class QdrantVectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    private final QdrantProperties props;
    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(props.getTimeoutMs());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.defaultHeader("api-key", props.getApiKey());
        }
        this.restClient = builder.build();
    }

    /** Build the deterministic point ID for a logical document. */
    public static String pointId(SourceType type, Long id) {
        return UUID.nameUUIDFromBytes((type.name() + ":" + id).getBytes()).toString();
    }

    /** Ping the Qdrant root endpoint. Returns true on 2xx. */
    public boolean isReachable() {
        try {
            restClient.get().uri("/").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Qdrant unreachable at {}: {}", props.getBaseUrl(), e.getMessage());
            return false;
        }
    }

    /** Idempotently create the collection if it doesn't exist. */
    public void ensureCollection(int dimensions) {
        String name = props.getCollection();
        try {
            restClient.get().uri("/collections/{name}", name)
                    .retrieve().toBodilessEntity();
            return; // exists
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 404) {
                throw new QdrantException("Could not query collection: " + e.getStatusCode());
            }
        } catch (Exception e) {
            throw new QdrantException("Qdrant not reachable: " + e.getMessage());
        }

        // 404 → create
        Map<String, Object> vectors = new LinkedHashMap<>();
        vectors.put("size", dimensions);
        vectors.put("distance", "Cosine");
        Map<String, Object> body = Map.of("vectors", vectors);
        try {
            restClient.put().uri("/collections/{name}", name)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Created Qdrant collection '{}' (dim={}, Cosine).", name, dimensions);
        } catch (Exception e) {
            throw new QdrantException("Failed creating collection: " + e.getMessage());
        }
    }

    /** Number of points in the collection. {@code -1} if call fails. */
    public long count() {
        try {
            CountResponse r = restClient.post()
                    .uri("/collections/{name}/points/count", props.getCollection())
                    .body(Map.of("exact", true))
                    .retrieve()
                    .body(CountResponse.class);
            return r != null && r.getResult() != null ? r.getResult().getCount() : -1L;
        } catch (Exception e) {
            log.warn("Qdrant count failed: {}", e.getMessage());
            return -1L;
        }
    }

    /** Upsert one point. */
    public void upsert(SourceType type, Long sourceId, float[] vector, Map<String, Object> payload) {
        upsertBatch(List.of(new UpsertItem(type, sourceId, vector, payload)));
    }

    /** Upsert many points in a single request. */
    public void upsertBatch(List<UpsertItem> items) {
        if (items == null || items.isEmpty()) return;
        List<Map<String, Object>> points = new ArrayList<>(items.size());
        for (UpsertItem it : items) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", pointId(it.type(), it.sourceId()));
            p.put("vector", toList(it.vector()));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("source_type", it.type().name());
            payload.put("source_id", it.sourceId());
            if (it.payload() != null) payload.putAll(it.payload());
            p.put("payload", payload);
            points.add(p);
        }
        try {
            restClient.put()
                    .uri("/collections/{name}/points?wait=true", props.getCollection())
                    .body(Map.of("points", points))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Qdrant upsert failed {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new QdrantException("Upsert failed: " + e.getStatusCode());
        } catch (Exception e) {
            throw new QdrantException("Upsert failed: " + e.getMessage());
        }
    }

    /** Delete one logical document. */
    public void delete(SourceType type, Long sourceId) {
        try {
            restClient.post()
                    .uri("/collections/{name}/points/delete?wait=true", props.getCollection())
                    .body(Map.of("points", List.of(pointId(type, sourceId))))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // 404 collection-missing is fine — treat as already gone.
            if (e.getStatusCode().value() == 404) return;
            log.warn("Qdrant delete error {}: {}", e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("Qdrant delete failed: {}", e.getMessage());
        }
    }

    /**
     * Top-K nearest neighbors. Optional payload filter (e.g. restrict to certain
     * source types or project IDs). Returns hits sorted by descending score.
     */
    public List<Hit> search(float[] query, int topK, Map<String, Object> filter) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", toList(query));
        body.put("limit", topK);
        body.put("with_payload", true);
        if (filter != null && !filter.isEmpty()) body.put("filter", filter);
        try {
            SearchResponse resp = restClient.post()
                    .uri("/collections/{name}/points/search", props.getCollection())
                    .body(body)
                    .retrieve()
                    .body(SearchResponse.class);
            if (resp == null || resp.getResult() == null) return Collections.emptyList();
            return resp.getResult();
        } catch (RestClientResponseException e) {
            throw new QdrantException("Search HTTP " + e.getStatusCode() + ": " + e.getMessage());
        } catch (Exception e) {
            throw new QdrantException("Search failed: " + e.getMessage());
        }
    }

    private static List<Float> toList(float[] arr) {
        List<Float> out = new ArrayList<>(arr.length);
        for (float v : arr) out.add(v);
        return out;
    }

    public record UpsertItem(SourceType type, Long sourceId, float[] vector, Map<String, Object> payload) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hit {
        private String id;
        private Double score;
        private Map<String, Object> payload;

        public SourceType sourceType() {
            Object o = payload != null ? payload.get("source_type") : null;
            return o != null ? SourceType.valueOf(o.toString()) : null;
        }
        public Long sourceId() {
            Object o = payload != null ? payload.get("source_id") : null;
            if (o instanceof Number n) return n.longValue();
            if (o != null) {
                try { return Long.parseLong(o.toString()); } catch (NumberFormatException ignore) {}
            }
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SearchResponse {
        private List<Hit> result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CountResponse {
        private CountResult result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CountResult {
        private long count;
    }

    public static class QdrantException extends RuntimeException {
        public QdrantException(String message) { super(message); }
    }
}
