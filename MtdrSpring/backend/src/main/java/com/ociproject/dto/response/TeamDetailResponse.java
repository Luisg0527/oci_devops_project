package com.ociproject.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamDetailResponse {
    private Long teamId;
    private String name;
    private String description;
    private List<TeamMemberSummary> members;

    @Data
    @Builder
    public static class TeamMemberSummary {
        private Long userId;
        private String fullName;
        private String roleName;
    }
}
