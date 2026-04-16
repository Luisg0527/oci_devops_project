package com.ociproject.dto.response;

import com.ociproject.model.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleResponse {
    private Long roleId;
    private String roleName;
    private String description;

    public static RoleResponse from(Role role) {
        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .build();
    }
}
