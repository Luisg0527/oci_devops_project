package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.CreateSprintRequest;
import com.ociproject.dto.request.UpdateSprintRequest;
import com.ociproject.dto.response.SprintResponse;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Sprints", description = "Sprint CRUD and task summary")
@RestController
@RequestMapping("/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "project_id", required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<Sprint> sprints;
        if (projectId != null) {
            List<ProjectSprint> projectSprints = projectService.findSprints(projectId);
            sprints = projectSprints.stream()
                    .map(ProjectSprint::getSprint)
                    .collect(Collectors.toList());
            if (status != null) {
                Sprint.Status s = Sprint.Status.valueOf(status);
                sprints = sprints.stream().filter(sp -> sp.getStatus() == s).collect(Collectors.toList());
            }
        } else if (status != null) {
            sprints = sprintService.findByStatus(Sprint.Status.valueOf(status));
        } else {
            sprints = sprintService.findAll();
        }

        List<SprintResponse> data = sprints.stream()
                .map(sp -> {
                    SprintResponse r = SprintResponse.from(sp);
                    if (projectId != null) {
                        List<ProjectSprint> psList = projectService.findSprints(projectId);
                        psList.stream()
                                .filter(ps -> ps.getSprint().getSprintId().equals(sp.getSprintId()))
                                .findFirst()
                                .ifPresent(ps -> {
                                    r.setProjectId(projectId);
                                    r.setSprintNumber(ps.getSprintNumber());
                                });
                    }
                    enrichSprintWithTasks(sp, r);
                    return r;
                })
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<SprintResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<SprintResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> getById(@PathVariable Long sprintId) {
        Sprint sprint = sprintService.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found."));
        SprintResponse response = SprintResponse.from(sprint);
        enrichSprintWithTasks(sprint, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateSprintRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("END_DATE must be greater than or equal to START_DATE.");
        }

        Sprint sprint = Sprint.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null
                        ? Sprint.Status.valueOf(request.getStatus())
                        : Sprint.Status.PLANNED)
                .deleted(false)
                .build();
        sprint = sprintService.save(sprint);
        auditLogService.log(actor, "CREATE", "SPRINTS", sprint.getSprintId(), httpRequest.getRemoteAddr());

        SprintResponse response = SprintResponse.from(sprint);
        response.setProjectId(request.getProjectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> update(@PathVariable Long sprintId,
                                                 @RequestBody UpdateSprintRequest request,
                                                 @AuthenticationPrincipal User actor,
                                                 HttpServletRequest httpRequest) {
        Sprint sprint = sprintService.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found."));

        if (request.getName() != null) sprint.setName(request.getName());
        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) sprint.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            sprint.setStatus(Sprint.Status.valueOf(request.getStatus()));
        }

        sprint = sprintService.save(sprint);
        auditLogService.log(actor, "UPDATE", "SPRINTS", sprintId, httpRequest.getRemoteAddr());

        SprintResponse response = SprintResponse.from(sprint);
        enrichSprintWithTasks(sprint, response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sprintId}")
    public ResponseEntity<?> delete(@PathVariable Long sprintId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        sprintService.softDelete(sprintId);
        auditLogService.log(actor, "DELETE", "SPRINTS", sprintId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Sprint soft-deleted successfully.",
                "sprint_id", sprintId
        ));
    }

    private void enrichSprintWithTasks(Sprint sprint, SprintResponse response) {
        List<Task> tasks = taskService.findBySprint(sprint.getSprintId());
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
        int inProgress = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count();
        int pending = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.PENDING).count();

        response.setTotalTasks(total);
        response.setCompletedTasks(completed);
        response.setInProgressTasks(inProgress);
        response.setPendingTasks(pending);
        response.setTotalStoryPoints(total);
        response.setCompletedStoryPoints(completed);
        response.setVelocityPercent(total > 0 ? (int) ((double) completed / total * 100) : 0);
        response.setOnTrack(response.getVelocityPercent() >= 50);
    }
}
