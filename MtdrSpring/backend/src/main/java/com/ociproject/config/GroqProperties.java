package com.ociproject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "groq")
@Getter
@Setter
public class GroqProperties {
    private String apiKey;
    private String apiUrl;
    private String model;
    private int timeoutMs = 20_000;
    private int maxContextChars = 120_000;
}
