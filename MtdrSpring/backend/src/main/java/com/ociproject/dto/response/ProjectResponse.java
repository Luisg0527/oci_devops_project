package com.ociproject.dto.response;

import com.ociproject.model.Project;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long projectId;
    private String name;
    private String description;
    private Long managerId;
    private String managerName;
    private String status;
    private Long activeSprintId;
    private String activeSprintName;
    private Integer memberCount;
    private String roleInProject;
    private Integer pendingTasksCount;
    private BigDecimal totalHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project, Long activeSprintId,
                                        String activeSprintName, int memberCount) {
        return from(project, activeSprintId, activeSprintName, memberCount, null, null);
    }

    public static ProjectResponse from(Project project, Long activeSprintId,
                                        String activeSprintName, int memberCount,
                                        String roleInProject, Integer pendingTasksCount) {
        return ProjectResponse.builder()
                .projectId(project.getProjectId())
                .name(project.getName())
                .description(project.getDescription())
                .managerId(project.getManager() != null ? project.getManager().getUserId() : null)
                .managerName(project.getManager() != null ? project.getManager().getFullName() : null)
                .status(project.getStatus() != null ? project.getStatus().name() : null)
                .activeSprintId(activeSprintId)
                .activeSprintName(activeSprintName)
                .memberCount(memberCount)
                .roleInProject(roleInProject)
                .pendingTasksCount(pendingTasksCount)
                .totalHours(project.getTotalHours())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
