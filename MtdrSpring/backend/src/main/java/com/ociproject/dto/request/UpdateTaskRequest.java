package com.ociproject.dto.request;

import lombok.Data;

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
}
