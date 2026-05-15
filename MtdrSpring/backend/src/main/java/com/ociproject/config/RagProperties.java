package com.ociproject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RagProperties {
    /** Master switch. When false, /ai/chat behaves exactly as before (full snapshot). */
    private boolean enabled = true;
    /** Number of nearest neighbors fetched from Qdrant. */
    private int topK = 20;
    /** How many retrieved docs (post-rerank) are hydrated and sent to the LLM. */
    private int hydratedLimit = 8;
    /** Re-index everything on startup if the Qdrant collection is empty. */
    private boolean bootstrapOnStartup = true;
}
