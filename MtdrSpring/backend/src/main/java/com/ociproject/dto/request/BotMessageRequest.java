package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BotMessageRequest {
    @NotNull private Long userId;
    @NotBlank private String message;
}
