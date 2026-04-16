package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.AddProjectMemberRequest;
import com.ociproject.dto.request.CreateProjectRequest;
import com.ociproject.dto.request.UpdateProjectRequest;
import com.ociproject.dto.response.ProjectMemberResponse;
import com.ociproject.dto.response.ProjectResponse;
import com.ociproject.exception.ResourceNotFoundException;
import com.ociproject.model.*;
import com.ociproject.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Projects", description = "Project CRUD, members, and health metrics")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final SprintService sprintService;
    private final TaskService taskService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(name = "manager_id", required = false) Long managerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<Project> projects;
        if (status != null) {
            projects = projectService.findByStatus(Project.Status.valueOf(status));
        } else if (managerId != null) {
            projects = projectService.findByManager(managerId);
        } else {
            projects = projectService.findAll();
        }

        List<ProjectResponse> data = projects.stream()
                .map(p -> {
                    List<ProjectSprint> sprints = projectService.findSprints(p.getProjectId());
                    ProjectSprint activeSprint = sprints.stream()
                            .filter(ps -> Boolean.TRUE.equals(ps.getActive()))
                            .findFirst().orElse(null);
                    int memberCount = projectService.findMembers(p.getProjectId()).size();
                    return ProjectResponse.from(p,
                            activeSprint != null ? activeSprint.getSprint().getSprintId() : null,
                            activeSprint != null ? activeSprint.getSprint().getName() : null,
                            memberCount);
                })
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<ProjectResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<ProjectResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        List<ProjectSprint> sprints = projectService.findSprints(projectId);
        ProjectSprint activeSprint = sprints.stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getActive()))
                .findFirst().orElse(null);
        int memberCount = projectService.findMembers(projectId).size();

        ProjectResponse response = ProjectResponse.from(project,
                activeSprint != null ? activeSprint.getSprint().getSprintId() : null,
                activeSprint != null ? activeSprint.getSprint().getName() : null,
                memberCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProjectRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        User manager = userService.findById(request.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found."));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .manager(manager)
                .status(request.getStatus() != null
                        ? Project.Status.valueOf(request.getStatus())
                        : Project.Status.ACTIVE)
                .deleted(false)
                .build();
        project = projectService.save(project);
        auditLogService.log(actor, "CREATE", "PROJECTS", project.getProjectId(), httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project, null, null, 0));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<?> update(@PathVariable Long projectId,
                                    @RequestBody UpdateProjectRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getManagerId() != null) {
            User manager = userService.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found."));
            project.setManager(manager);
        }
        if (request.getStatus() != null) {
            project.setStatus(Project.Status.valueOf(request.getStatus()));
        }

        project = projectService.save(project);
        auditLogService.log(actor, "UPDATE", "PROJECTS", projectId, httpRequest.getRemoteAddr());

        List<ProjectSprint> sprints = projectService.findSprints(projectId);
        ProjectSprint activeSprint = sprints.stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getActive()))
                .findFirst().orElse(null);
        int memberCount = projectService.findMembers(projectId).size();

        return ResponseEntity.ok(ProjectResponse.from(project,
                activeSprint != null ? activeSprint.getSprint().getSprintId() : null,
                activeSprint != null ? activeSprint.getSprint().getName() : null,
                memberCount));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        projectService.softDelete(projectId);
        auditLogService.log(actor, "DELETE", "PROJECTS", projectId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Project soft-deleted successfully.",
                "project_id", projectId,
                "deleted_at", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long projectId) {
        List<ProjectMember> members = projectService.findMembers(projectId);
        List<ProjectMemberResponse> data = members.stream()
                .map(ProjectMemberResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "project_id", projectId,
                "total", data.size(),
                "members", data
        ));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long projectId,
                                       @Valid @RequestBody AddProjectMemberRequest request,
                                       @AuthenticationPrincipal User actor,
                                       HttpServletRequest httpRequest) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        ProjectMember member = projectService.addMember(projectId, user, request.getRoleInProject());
        auditLogService.log(actor, "CREATE", "PROJECT_MEMBERS", projectId, httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMemberResponse.from(member));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long projectId,
                                          @PathVariable Long userId,
                                          @AuthenticationPrincipal User actor,
                                          HttpServletRequest httpRequest) {
        projectService.removeMember(projectId, userId);
        auditLogService.log(actor, "DELETE", "PROJECT_MEMBERS", projectId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Member removed from project.",
                "project_id", projectId,
                "user_id", userId
        ));
    }

    @GetMapping("/{projectId}/health")
    public ResponseEntity<?> getHealth(@PathVariable Long projectId) {
        projectService.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        List<Task> tasks = taskService.findByProject(projectId);
        long totalTasks = tasks.size();
        long doneTasks = tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
        double onTimeRate = totalTasks > 0 ? ((double) doneTasks / totalTasks) * 100 : 0;
        long overdue = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(java.time.LocalDate.now())
                        && t.getStatus() != Task.Status.DONE
                        && t.getStatus() != Task.Status.CANCELLED)
                .count();

        String healthLabel;
        if (onTimeRate >= 80) healthLabel = "Excellent";
        else if (onTimeRate >= 60) healthLabel = "Good";
        else if (onTimeRate >= 40) healthLabel = "Fair";
        else healthLabel = "At Risk";

        return ResponseEntity.ok(Map.of(
                "project_id", projectId,
                "on_time_delivery_rate", Math.round(onTimeRate * 100.0) / 100.0,
                "tickets_resolved_7d", doneTasks,
                "efficiency_rating", onTimeRate >= 80 ? "ELITE" : onTimeRate >= 60 ? "HIGH" : "MEDIUM",
                "overdue_tasks_this_week", overdue,
                "project_health_label", healthLabel
        ));
    }
}
