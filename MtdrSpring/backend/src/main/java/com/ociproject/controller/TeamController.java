package com.ociproject.controller;

import com.ociproject.dto.DataResponse;
import com.ociproject.dto.request.CreateTeamRequest;
import com.ociproject.dto.request.UpdateTeamRequest;
import com.ociproject.dto.response.TeamDetailResponse;
import com.ociproject.dto.response.TeamResponse;
import com.ociproject.exception.ResourceNotFoundException;
import com.ociproject.model.Team;
import com.ociproject.model.User;
import com.ociproject.service.AuditLogService;
import com.ociproject.service.TeamService;
import com.ociproject.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Teams", description = "Team CRUD and member listing")
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<DataResponse<TeamResponse>> getAll() {
        List<Team> teams = teamService.findAll();
        List<TeamResponse> data = teams.stream()
                .map(t -> TeamResponse.from(t, userService.findByTeam(t.getTeamId()).size()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(DataResponse.<TeamResponse>builder().data(data).build());
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getById(@PathVariable Long teamId) {
        Team team = teamService.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));
        List<User> members = userService.findByTeam(teamId);
        List<TeamDetailResponse.TeamMemberSummary> memberList = members.stream()
                .map(u -> TeamDetailResponse.TeamMemberSummary.builder()
                        .userId(u.getUserId())
                        .fullName(u.getFullName())
                        .roleName(u.getRole() != null ? u.getRole().getRoleName() : null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(TeamDetailResponse.builder()
                .teamId(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .members(memberList)
                .build());
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request,
                                               @AuthenticationPrincipal User actor,
                                               HttpServletRequest httpRequest) {
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .deleted(false)
                .build();
        team = teamService.save(team);
        auditLogService.log(actor, "CREATE", "TEAMS", team.getTeamId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(team, 0));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> update(@PathVariable Long teamId,
                                               @RequestBody UpdateTeamRequest request,
                                               @AuthenticationPrincipal User actor,
                                               HttpServletRequest httpRequest) {
        Team team = teamService.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));
        if (request.getName() != null) team.setName(request.getName());
        if (request.getDescription() != null) team.setDescription(request.getDescription());
        team = teamService.save(team);
        auditLogService.log(actor, "UPDATE", "TEAMS", teamId, httpRequest.getRemoteAddr());
        int memberCount = userService.findByTeam(teamId).size();
        return ResponseEntity.ok(TeamResponse.from(team, memberCount));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> delete(@PathVariable Long teamId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        teamService.softDelete(teamId);
        auditLogService.log(actor, "DELETE", "TEAMS", teamId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Team soft-deleted successfully."));
    }
}
