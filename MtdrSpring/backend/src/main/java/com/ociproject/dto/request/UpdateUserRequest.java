package com.ociproject.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private String telegramId;
    private Long roleId;
    private Long teamId;
    private String status;
}
