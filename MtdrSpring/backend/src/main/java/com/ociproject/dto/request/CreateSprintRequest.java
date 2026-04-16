package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSprintRequest {
    @NotBlank private String name;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String status;
    @NotNull private Long projectId;
}
