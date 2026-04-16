package com.ociproject.dto.response;

import com.ociproject.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String telegramId;
    private Long roleId;
    private String roleName;
    private Long teamId;
    private String teamName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .telegramId(user.getTelegramId())
                .roleId(user.getRole() != null ? user.getRole().getRoleId() : null)
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .teamId(user.getTeam() != null ? user.getTeam().getTeamId() : null)
                .teamName(user.getTeam() != null ? user.getTeam().getName() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isDeleted(user.getDeleted())
                .build();
    }
}
