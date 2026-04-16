package com.ociproject.dto.response;

import com.ociproject.model.Sprint;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SprintResponse {
    private Long sprintId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long projectId;
    private String projectName;
    private Integer sprintNumber;
    private Boolean isActive;
    private Long daysLeft;
    private Integer totalStoryPoints;
    private Integer completedStoryPoints;
    private Integer velocityPercent;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer inProgressTasks;
    private Integer pendingTasks;
    private Boolean onTrack;
    private LocalDateTime createdAt;

    public static SprintResponse from(Sprint sprint) {
        boolean active = sprint.getStatus() == Sprint.Status.ACTIVE;
        long daysLeft = 0;
        if (active && sprint.getEndDate() != null) {
            daysLeft = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), sprint.getEndDate()));
        }
        return SprintResponse.builder()
                .sprintId(sprint.getSprintId())
                .name(sprint.getName())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus() != null ? sprint.getStatus().name() : null)
                .isActive(active)
                .daysLeft(daysLeft)
                .createdAt(sprint.getCreatedAt())
                .build();
    }
}
