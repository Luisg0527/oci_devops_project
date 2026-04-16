package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateKpiTypeRequest {
    @NotBlank private String name;
    private String description;
    private String category;
    private String unit;
}
