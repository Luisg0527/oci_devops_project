package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatMessageDto {
    /** "user" o "assistant" */
    @NotBlank
    private String role;

    @NotBlank
    private String content;
}
