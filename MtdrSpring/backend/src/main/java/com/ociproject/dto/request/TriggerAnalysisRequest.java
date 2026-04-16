package com.ociproject.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TriggerAnalysisRequest {
    @NotNull private String scopeType;
    private Long userId;
    private Long projectId;
    private Long sprintId;
}
