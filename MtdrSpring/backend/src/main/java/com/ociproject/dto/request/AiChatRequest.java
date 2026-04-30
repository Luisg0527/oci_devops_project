package com.ociproject.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatRequest {

    @NotBlank
    @Size(max = 4000)
    private String message;

    /** Últimos N mensajes de la conversación, en orden cronológico. */
    @Valid
    @Size(max = 20)
    private List<AiChatMessageDto> history = new ArrayList<>();
}
