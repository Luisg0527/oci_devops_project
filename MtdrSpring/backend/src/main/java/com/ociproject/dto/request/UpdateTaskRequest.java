package com.ociproject.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTaskRequest {
    private String title;
    private String description;
    private Long sprintId;
    private String taskStage;
    private String status;
    private String priority;
    private Long assignedTo;
    private LocalDate dueDate;

    @DecimalMin(value = "0.01", message = "estimatedHours must be greater than 0")
    @DecimalMax(value = "4.00", message = "estimatedHours must be at most 4")
    private BigDecimal estimatedHours;

    private Long parentTaskId;
}
