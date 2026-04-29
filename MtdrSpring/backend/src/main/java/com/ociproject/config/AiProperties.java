package com.ociproject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private Deepseek deepseek = new Deepseek();
    private Oci oci = new Oci();
    private Rag rag = new Rag();

    @Getter @Setter
    public static class Deepseek {
        private String apiKey;
        private String apiUrl;
        private String model = "deepseek-chat";
        private int maxTokens = 800;
        private double temperature = 0.2;
    }

    @Getter @Setter
    public static class Oci {
        private String compartmentId;
        private String region;
        private String embeddingModelId = "cohere.embed-multilingual-v3.0";
        private String configFilePath;
        private String authProfile = "DEFAULT";
    }

    @Getter @Setter
    public static class Rag {
        private int topK = 8;
        private int maxContextChars = 12000;
        private int embedBatchSize = 96;
    }
}
