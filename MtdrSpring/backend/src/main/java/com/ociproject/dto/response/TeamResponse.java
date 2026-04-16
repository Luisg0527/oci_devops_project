package com.ociproject.dto.response;

import com.ociproject.model.Team;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamResponse {
    private Long teamId;
    private String name;
    private String description;
    private Integer memberCount;

    public static TeamResponse from(Team team, int memberCount) {
        return TeamResponse.builder()
                .teamId(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .memberCount(memberCount)
                .build();
    }
}
