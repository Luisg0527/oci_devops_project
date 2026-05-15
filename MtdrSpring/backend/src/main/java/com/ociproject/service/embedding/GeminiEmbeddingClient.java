package com.ociproject.service.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ociproject.config.GeminiProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Embedding client backed by Google's Gemini "text-embedding-004" model
 * (free tier, multilingual, 768-dim).
 *
 * Endpoints used:
 * - POST {apiUrl}/{model}:embedContent      (single text)
 * - POST {apiUrl}/{model}:batchEmbedContents (up to ~100 texts per request)
 *
 * API key is sent via the {@code ?key=...} query param, per Google's spec.
 */
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);

    private final GeminiProperties props;
    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(props.getTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(props.getApiUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public int dimensions() {
        return props.getDimensions();
    }

    @Override
    public boolean isConfigured() {
        String k = props.getApiKey();
        return k != null && !k.isBlank() && !"tu-key-aqui".equals(k);
    }

    @Override
    public float[] embed(String text) {
        if (!isConfigured()) {
            throw new EmbeddingException("Gemini API key no configurada (GEMINI_API_KEY).");
        }
        if (text == null || text.isBlank()) {
            return new float[props.getDimensions()];
        }
        String uri = "/" + props.getModel() + ":embedContent?key=" + props.getApiKey();
        SingleRequest body = new SingleRequest(props.getModel(),
                new Content(List.of(new Part(text))));
        try {
            SingleResponse resp = restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(SingleResponse.class);
            return toFloatArray(resp);
        } catch (RestClientResponseException e) {
            log.error("Gemini embed error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingException("Gemini API respondió " + e.getStatusCode());
        } catch (Exception e) {
            throw new EmbeddingException("No se pudo contactar a Gemini: " + e.getMessage());
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();
        if (!isConfigured()) {
            throw new EmbeddingException("Gemini API key no configurada (GEMINI_API_KEY).");
        }

        List<float[]> out = new ArrayList<>(texts.size());
        int batchSize = Math.max(1, props.getBatchSize());

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> chunk = texts.subList(i, Math.min(i + batchSize, texts.size()));
            out.addAll(embedChunk(chunk));
        }
        return out;
    }

    private List<float[]> embedChunk(List<String> chunk) {
        String uri = "/" + props.getModel() + ":batchEmbedContents?key=" + props.getApiKey();
        List<SingleRequest> requests = new ArrayList<>(chunk.size());
        for (String t : chunk) {
            String safe = (t == null || t.isBlank()) ? " " : t;
            requests.add(new SingleRequest(props.getModel(), new Content(List.of(new Part(safe)))));
        }
        BatchRequest body = new BatchRequest(requests);
        try {
            BatchResponse resp = restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(BatchResponse.class);
            if (resp == null || resp.getEmbeddings() == null) {
                throw new EmbeddingException("Gemini batch devolvió respuesta vacía.");
            }
            List<float[]> result = new ArrayList<>(chunk.size());
            for (Embedding emb : resp.getEmbeddings()) {
                result.add(toFloatArray(emb));
            }
            return result;
        } catch (RestClientResponseException e) {
            log.warn("Gemini batch failed ({}): {} — falling back to per-item.", e.getStatusCode(), e.getMessage());
            // Fallback: embed one-by-one so a single bad input doesn't kill the batch.
            List<float[]> result = new ArrayList<>(chunk.size());
            for (String t : chunk) {
                try {
                    result.add(embed(t));
                } catch (Exception inner) {
                    log.warn("Skipping text after Gemini single-embed failure: {}", inner.getMessage());
                    result.add(new float[props.getDimensions()]);
                }
            }
            return result;
        } catch (Exception e) {
            throw new EmbeddingException("No se pudo contactar a Gemini (batch): " + e.getMessage());
        }
    }

    private float[] toFloatArray(SingleResponse resp) {
        if (resp == null || resp.getEmbedding() == null) {
            throw new EmbeddingException("Gemini devolvió embedding vacío.");
        }
        return toFloatArray(resp.getEmbedding());
    }

    private float[] toFloatArray(Embedding emb) {
        List<Double> values = emb.getValues();
        if (values == null || values.isEmpty()) {
            throw new EmbeddingException("Gemini devolvió vector vacío.");
        }
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) out[i] = values.get(i).floatValue();
        return out;
    }

    // ── Request/Response DTOs (kept private; mirror Gemini's REST shape) ──

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SingleRequest {
        private final String model;
        private final Content content;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BatchRequest {
        private final List<SingleRequest> requests;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Content {
        private final List<Part> parts;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Part {
        private final String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SingleResponse {
        private Embedding embedding;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BatchResponse {
        private List<Embedding> embeddings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Embedding {
        private List<Double> values;
    }

    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String message) { super(message); }
    }
}
