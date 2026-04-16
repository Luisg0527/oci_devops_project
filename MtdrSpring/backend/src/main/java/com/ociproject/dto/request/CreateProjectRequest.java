package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotBlank private String name;
    private String description;
    @NotNull private Long managerId;
    private String status;
}
