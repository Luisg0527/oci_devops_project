package com.ociproject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "qdrant")
@Getter
@Setter
public class QdrantProperties {
    private String baseUrl;
    private String apiKey;
    private String collection;
    private int timeoutMs = 10_000;
}
