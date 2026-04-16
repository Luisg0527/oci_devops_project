package com.ociproject.dto.response;

import com.ociproject.model.Task;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskResponse {
    private Long taskId;
    private Long projectId;
    private String projectName;
    private Long sprintId;
    private String sprintName;
    private String title;
    private String description;
    private String taskStage;
    private String status;
    private String priority;
    private Long createdBy;
    private String creatorName;
    private Long assignedTo;
    private String assigneeName;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private List<StatusHistoryEntry> statusHistory;
    private List<SprintHistoryEntry> sprintHistory;

    @Data
    @Builder
    public static class StatusHistoryEntry {
        private Long historyId;
        private String oldStatus;
        private String newStatus;
        private Long changedBy;
        private String changedByName;
        private LocalDateTime changedAt;
    }

    @Data
    @Builder
    public static class SprintHistoryEntry {
        private Long historyId;
        private Long oldSprintId;
        private String oldSprintName;
        private Long newSprintId;
        private String newSprintName;
        private Long changedBy;
        private String changedByName;
        private LocalDateTime changedAt;
    }

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .projectId(task.getProject() != null ? task.getProject().getProjectId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .sprintId(task.getSprint() != null ? task.getSprint().getSprintId() : null)
                .sprintName(task.getSprint() != null ? task.getSprint().getName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .taskStage(task.getTaskStage() != null ? task.getTaskStage().name() : null)
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .createdBy(task.getCreatedBy() != null ? task.getCreatedBy().getUserId() : null)
                .creatorName(task.getCreatedBy() != null ? task.getCreatedBy().getFullName() : null)
                .assignedTo(task.getAssignedTo() != null ? task.getAssignedTo().getUserId() : null)
                .assigneeName(task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .isDeleted(task.getDeleted())
                .build();
    }
}
