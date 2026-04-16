package com.ociproject.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Long teamId;
    private String expiresAt;
}
