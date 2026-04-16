package com.ociproject.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank private String fullName;
    @NotBlank @Email private String email;
    private String telegramId;
    @NotNull private Long roleId;
    private Long teamId;
    @NotBlank private String username;
    @NotBlank private String password;
}
