package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
}
