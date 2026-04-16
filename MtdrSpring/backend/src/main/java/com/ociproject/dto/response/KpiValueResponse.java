package com.ociproject.dto.response;

import com.ociproject.model.KpiValue;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class KpiValueResponse {
    private Long kpiValueId;
    private Long kpiTypeId;
    private String kpiName;
    private String category;
    private String unit;
    private String scopeType;
    private Long sprintId;
    private String sprintName;
    private Long projectId;
    private Long userId;
    private BigDecimal value;
    private LocalDateTime recordedAt;

    public static KpiValueResponse from(KpiValue kv) {
        return KpiValueResponse.builder()
                .kpiValueId(kv.getKpiValueId())
                .kpiTypeId(kv.getKpiType() != null ? kv.getKpiType().getKpiTypeId() : null)
                .kpiName(kv.getKpiType() != null ? kv.getKpiType().getName() : null)
                .category(kv.getKpiType() != null ? kv.getKpiType().getCategory() : null)
                .unit(kv.getKpiType() != null ? kv.getKpiType().getUnit() : null)
                .scopeType(kv.getScopeType() != null ? kv.getScopeType().name() : null)
                .sprintId(kv.getSprint() != null ? kv.getSprint().getSprintId() : null)
                .sprintName(kv.getSprint() != null ? kv.getSprint().getName() : null)
                .projectId(kv.getProject() != null ? kv.getProject().getProjectId() : null)
                .userId(kv.getUser() != null ? kv.getUser().getUserId() : null)
                .value(kv.getValue())
                .recordedAt(kv.getRecordedAt())
                .build();
    }
}
