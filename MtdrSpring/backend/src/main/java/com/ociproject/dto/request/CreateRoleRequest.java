package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    @NotBlank private String roleName;
    private String description;
}
