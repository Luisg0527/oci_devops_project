package com.ociproject.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateKpiValueRequest {
    private BigDecimal value;
    private LocalDateTime recordedAt;
}
