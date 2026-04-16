package com.ociproject.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateDeploymentRequest {
    @NotNull private Long projectId;
    private String version;
    private String environment;
    private String status;
    private LocalDateTime deployedAt;
}
