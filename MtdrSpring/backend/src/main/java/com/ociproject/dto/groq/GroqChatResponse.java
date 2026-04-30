package com.ociproject.dto.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqChatResponse {
    private String id;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private int index;
        private GroqMessage message;
        @com.fasterxml.jackson.annotation.JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @com.fasterxml.jackson.annotation.JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @com.fasterxml.jackson.annotation.JsonProperty("completion_tokens")
        private Integer completionTokens;
        @com.fasterxml.jackson.annotation.JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
