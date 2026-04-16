package com.ociproject.dto.request;

import lombok.Data;

@Data
public class UpdateKpiTypeRequest {
    private String name;
    private String description;
    private String category;
    private String unit;
}
