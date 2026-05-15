package com.ociproject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiProperties {
    private String apiKey;
    private String apiUrl;
    private String model;
    private int dimensions = 768;
    private int batchSize = 100;
    private int timeoutMs = 15_000;
}
