package com.ociproject.dto.response;

import com.ociproject.model.KpiType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiTypeResponse {
    private Long kpiTypeId;
    private String name;
    private String description;
    private String category;
    private String unit;

    public static KpiTypeResponse from(KpiType kt) {
        return KpiTypeResponse.builder()
                .kpiTypeId(kt.getKpiTypeId())
                .name(kt.getName())
                .description(kt.getDescription())
                .category(kt.getCategory())
                .unit(kt.getUnit())
                .build();
    }
}
