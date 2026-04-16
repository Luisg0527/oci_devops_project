package com.ociproject.dto.request;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String roleName;
    private String description;
}
