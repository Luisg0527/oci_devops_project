package com.ociproject.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDeploymentRequest {
    private String status;
    private BigDecimal recoveryTimeMin;
}
