package com.ociproject.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatResponse {
    private String reply;
    private String model;
    private long elapsedMs;
}
