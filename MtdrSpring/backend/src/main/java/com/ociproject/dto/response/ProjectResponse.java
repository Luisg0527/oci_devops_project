package com.ociproject.dto.response;

import com.ociproject.model.Project;
import lombok.Builder;
import lombok.Data;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project, Long activeSprintId,
                                        String activeSprintName, int memberCount) {
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
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
