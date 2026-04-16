package com.ociproject.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkloadResponse {
    private Long userId;
    private String fullName;
    private Integer assignedTasksTotal;
    private Integer inProgressTasks;
    private Integer pendingTasks;
    private Integer doneTasks;
    private Integer workloadPercent;
    private Boolean atCapacity;
}
