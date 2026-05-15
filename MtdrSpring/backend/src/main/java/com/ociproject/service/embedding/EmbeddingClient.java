package com.ociproject.service.embedding;

import java.util.List;

/**
 * Provider-agnostic embedding interface. Swap implementations (Gemini, Jina,
 * self-hosted ONNX, etc.) without touching the rest of the RAG pipeline.
 */
public interface EmbeddingClient {

    /** Embed a single text. Returns a vector of {@link #dimensions()} length. */
    float[] embed(String text);

    /**
     * Embed several texts in one round-trip when the provider supports it.
     * Implementations may fall back to N sequential calls.
     */
    List<float[]> embedBatch(List<String> texts);

    /** Dimensionality produced by the underlying model. */
    int dimensions();

    /** Whether the client has the credentials it needs to talk to its backend. */
    boolean isConfigured();
}
