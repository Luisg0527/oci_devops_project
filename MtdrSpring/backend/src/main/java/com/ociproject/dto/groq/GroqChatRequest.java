package com.ociproject.dto.groq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroqChatRequest {
    private String model;
    private List<GroqMessage> messages;
    private Double temperature;
    @com.fasterxml.jackson.annotation.JsonProperty("max_tokens")
    private Integer maxTokens;
    private Boolean stream;
}
