package com.ociproject.dto.response;

import com.ociproject.model.ProjectMember;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectMemberResponse {
    private Long projectId;
    private Long userId;
    private String fullName;
    private String email;
    private String roleInProject;
    private LocalDateTime joinedAt;
    private Boolean isDeleted;

    public static ProjectMemberResponse from(ProjectMember pm) {
        return ProjectMemberResponse.builder()
                .projectId(pm.getId().getProjectId())
                .userId(pm.getId().getUserId())
                .fullName(pm.getUser() != null ? pm.getUser().getFullName() : null)
                .email(pm.getUser() != null ? pm.getUser().getEmail() : null)
                .roleInProject(pm.getRoleInProject())
                .joinedAt(pm.getJoinedAt())
                .isDeleted(pm.getDeleted())
                .build();
    }
}
