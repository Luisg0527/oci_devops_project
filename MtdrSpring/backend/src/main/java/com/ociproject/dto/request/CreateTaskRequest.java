package com.ociproject.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTaskRequest {
    @NotNull private Long projectId;
    private Long sprintId;
    @NotBlank private String title;
    private String description;
    private String taskStage;
    private String status;
    private String priority;
    @NotNull private Long createdBy;
    private Long assignedTo;
    private LocalDate dueDate;

    @DecimalMin(value = "0.01", message = "estimatedHours must be greater than 0")
    @DecimalMax(value = "4.00", message = "estimatedHours must be at most 4")
    private BigDecimal estimatedHours;

    private Boolean isSubtask;
    private Long parentTaskId;
}
