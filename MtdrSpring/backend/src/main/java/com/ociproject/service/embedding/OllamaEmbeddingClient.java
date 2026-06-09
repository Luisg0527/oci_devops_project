package com.ociproject.service.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ociproject.config.GeminiProperties;
import com.ociproject.service.embedding.GeminiEmbeddingClient.EmbeddingException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Embedding client backed by a local <a href="https://ollama.com">Ollama</a> server
 * running {@code nomic-embed-text} (768-dim, aligned with the Qdrant collection).
 *
 * Reuses the {@code gemini.*} configuration namespace so swapping providers is a
 * config-only change:
 * <ul>
 *   <li>{@code gemini.api-url}   → Ollama base URL (e.g. {@code http://localhost:11434})</li>
 *   <li>{@code gemini.model}     → embedding model (e.g. {@code nomic-embed-text})</li>
 *   <li>{@code gemini.dimensions}→ vector length (768 for nomic-embed-text)</li>
 * </ul>
 *
 * Endpoint used: {@code POST {apiUrl}/api/embeddings} with {@code {"model","prompt"}},
 * returning {@code {"embedding":[...]}}. Ollama has no multi-text batch endpoint, so
 * {@link #embedBatch(List)} issues one request per text.
 *
 * Marked {@link Primary} so Spring prefers it over {@link GeminiEmbeddingClient}.
 */
@Service
@Primary
@RequiredArgsConstructor
public class OllamaEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

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
        log.info("OllamaEmbeddingClient ready — {} ({}d) at {}",
                props.getModel(), props.getDimensions(), props.getApiUrl());
    }

    @Override
    public int dimensions() {
        return props.getDimensions();
    }

    @Override
    public boolean isConfigured() {
        // Ollama needs no API key; a base URL and model name are enough.
        return props.getApiUrl() != null && !props.getApiUrl().isBlank()
                && props.getModel() != null && !props.getModel().isBlank();
    }

    @Override
    public float[] embed(String text) {
        if (!isConfigured()) {
            throw new EmbeddingException("Ollama no configurado (gemini.api-url / gemini.model).");
        }
        if (text == null || text.isBlank()) {
            return new float[props.getDimensions()];
        }
        return callOllama(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();
        if (!isConfigured()) {
            throw new EmbeddingException("Ollama no configurado (gemini.api-url / gemini.model).");
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) {
            String safe = (t == null || t.isBlank()) ? " " : t;
            try {
                out.add(callOllama(safe));
            } catch (Exception e) {
                log.warn("Skipping text after Ollama embed failure: {}", e.getMessage());
                out.add(new float[props.getDimensions()]);
            }
        }
        return out;
    }

    private float[] callOllama(String text) {
        try {
            OllamaResponse resp = restClient.post()
                    .uri("/api/embeddings")
                    .body(new OllamaRequest(props.getModel(), text))
                    .retrieve()
                    .body(OllamaResponse.class);
            return toFloatArray(resp);
        } catch (RestClientResponseException e) {
            log.error("Ollama embed error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingException("Ollama respondió " + e.getStatusCode());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("No se pudo contactar a Ollama: " + e.getMessage());
        }
    }

    private float[] toFloatArray(OllamaResponse resp) {
        if (resp == null || resp.getEmbedding() == null || resp.getEmbedding().isEmpty()) {
            throw new EmbeddingException("Ollama devolvió embedding vacío.");
        }
        List<Double> values = resp.getEmbedding();
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) out[i] = values.get(i).floatValue();
        return normalize(out);
    }

    /** L2-normalize so cosine similarity in Qdrant behaves consistently. */
    private float[] normalize(float[] v) {
        double norm = 0;
        for (float f : v) norm += (double) f * f;
        if (norm == 0) return v;
        float scale = (float) (1.0 / Math.sqrt(norm));
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] * scale;
        return out;
    }

    // ── Request/Response DTOs (mirror Ollama's /api/embeddings shape) ──

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class OllamaRequest {
        private final String model;
        private final String prompt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OllamaResponse {
        private List<Double> embedding;
    }
}
