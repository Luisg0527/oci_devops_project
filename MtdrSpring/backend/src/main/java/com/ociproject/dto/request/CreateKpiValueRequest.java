package com.ociproject.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateKpiValueRequest {
    @NotNull private Long kpiTypeId;
    @NotNull private String scopeType;
    private Long userId;
    private Long projectId;
    private Long sprintId;
    @NotNull private BigDecimal value;
    private LocalDateTime recordedAt;
}
