package com.ociproject.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddProjectMemberRequest {
    @NotNull private Long userId;
    private String roleInProject;
}
