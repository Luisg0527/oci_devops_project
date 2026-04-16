package com.ociproject.dto.response;

import com.ociproject.model.BotInteraction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BotInteractionResponse {
    private Long interactionId;
    private Long userId;
    private String userName;
    private String message;
    private String response;
    private LocalDateTime createdAt;

    public static BotInteractionResponse from(BotInteraction bi) {
        return BotInteractionResponse.builder()
                .interactionId(bi.getId() != null ? bi.getId().getInteractionId() : null)
                .userId(bi.getUser() != null ? bi.getUser().getUserId() : null)
                .userName(bi.getUser() != null ? bi.getUser().getFullName() : null)
                .message(bi.getMessage())
                .response(bi.getResponse())
                .createdAt(bi.getId() != null ? bi.getId().getCreatedAt() : null)
                .build();
    }
}
