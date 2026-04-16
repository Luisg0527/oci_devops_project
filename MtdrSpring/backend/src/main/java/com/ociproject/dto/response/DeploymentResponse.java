package com.ociproject.dto.response;

import com.ociproject.model.Deployment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DeploymentResponse {
    private Long deploymentId;
    private Long projectId;
    private String projectName;
    private String version;
    private String environment;
    private String status;
    private LocalDateTime deployedAt;
    private BigDecimal recoveryTimeMin;

    public static DeploymentResponse from(Deployment d) {
        return DeploymentResponse.builder()
                .deploymentId(d.getDeploymentId())
                .projectId(d.getProject() != null ? d.getProject().getProjectId() : null)
                .projectName(d.getProject() != null ? d.getProject().getName() : null)
                .version(d.getVersion())
                .environment(d.getEnvironment() != null ? d.getEnvironment().name() : null)
                .status(d.getStatus() != null ? d.getStatus().name() : null)
                .deployedAt(d.getDeployedAt())
                .recoveryTimeMin(d.getRecoveryTimeMin())
                .build();
    }
}
