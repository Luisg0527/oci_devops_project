package com.ociproject.controller;

import com.ociproject.model.*;
import com.ociproject.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Reports", description = "Burndown, velocity, cumulative flow, milestones, and PDF export")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SprintService sprintService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    @GetMapping("/burndown")
    public ResponseEntity<?> getBurndown(@RequestParam("sprint_id") Long sprintId) {
        Sprint sprint = sprintService.findById(sprintId)
                .orElseThrow(() -> new com.ociproject.exception.ResourceNotFoundException("Sprint not found."));

        List<Task> tasks = taskService.findBySprint(sprintId);
        int totalPoints = tasks.size();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();

        long totalDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate());
        if (totalDays <= 0) totalDays = 7;

        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<Map<String, Object>> idealLine = new ArrayList<>();
        List<Map<String, Object>> actualLine = new ArrayList<>();

        for (int i = 0; i < Math.min(dayLabels.length, totalDays + 1); i++) {
            double idealRemaining = totalPoints - ((double) totalPoints / totalDays * i);
            idealLine.add(Map.of("day", dayLabels[i], "ideal_remaining", Math.max(0, (int) idealRemaining)));

            // Approximate actual remaining
            double actualRemaining = totalPoints - ((double) completed / Math.max(1, totalDays) * i * totalDays / completed);
            if (completed == 0) actualRemaining = totalPoints;
            actualLine.add(Map.of("day", dayLabels[i], "actual_remaining", Math.max(0, (int) Math.round(actualRemaining))));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sprint_id", sprintId);
        response.put("sprint_name", sprint.getName());
        response.put("on_track", completed >= totalPoints / 2);
        response.put("total_points", totalPoints);
        response.put("ideal_line", idealLine);
        response.put("actual_line", actualLine);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sprint-velocity")
    public ResponseEntity<?> getSprintVelocity(
            @RequestParam("project_id") Long projectId,
            @RequestParam(name = "last_n", defaultValue = "6") int lastN) {

        List<ProjectSprint> projectSprints = projectService.findSprints(projectId);
        List<ProjectSprint> sorted = projectSprints.stream()
                .sorted(Comparator.comparingInt(ProjectSprint::getSprintNumber))
                .collect(Collectors.toList());

        int startIdx = Math.max(0, sorted.size() - lastN);
        List<ProjectSprint> recent = sorted.subList(startIdx, sorted.size());

        List<Map<String, Object>> sprintData = new ArrayList<>();
        int totalPoints = 0;
        for (int i = 0; i < recent.size(); i++) {
            ProjectSprint ps = recent.get(i);
            Sprint sprint = ps.getSprint();
            List<Task> tasks = taskService.findBySprint(sprint.getSprintId());
            int completed = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
            totalPoints += completed;
            boolean isCurrent = i == recent.size() - 1;

            sprintData.add(Map.of(
                    "sprint_id", sprint.getSprintId(),
                    "label", isCurrent ? "CURR" : "SPR " + ps.getSprintNumber(),
                    "points_delivered", completed,
                    "is_current", isCurrent
            ));
        }

        double avgVelocity = sprintData.isEmpty() ? 0 : (double) totalPoints / sprintData.size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("project_id", projectId);
        response.put("current_velocity", Math.round(avgVelocity * 10.0) / 10.0);
        response.put("change_vs_last_pct", 0);
        response.put("sprints", sprintData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cumulative-flow")
    public ResponseEntity<?> getCumulativeFlow(
            @RequestParam("project_id") Long projectId,
            @RequestParam(defaultValue = "7") int weeks) {

        List<Task> tasks = taskService.findByProject(projectId);
        int backlog = (int) tasks.stream().filter(t -> t.getTaskStage() == Task.Stage.BACKLOG).count();
        int inProgress = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count();
        int done = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();

        List<Map<String, Object>> weekData = new ArrayList<>();
        for (int i = 1; i <= weeks; i++) {
            weekData.add(Map.of(
                    "week", "W" + i,
                    "backlog", Math.max(0, backlog - i + 1),
                    "in_progress", inProgress,
                    "done", Math.min(done + i - 1, tasks.size())
            ));
        }

        return ResponseEntity.ok(Map.of(
                "project_id", projectId,
                "weeks", weekData
        ));
    }

    @GetMapping("/task-completion")
    public ResponseEntity<?> getTaskCompletion(
            @RequestParam(name = "project_id", required = false) Long projectId,
            @RequestParam(name = "sprint_id", required = false) Long sprintId) {

        List<Task> tasks;
        if (sprintId != null) {
            tasks = taskService.findBySprint(sprintId);
        } else if (projectId != null) {
            tasks = taskService.findByProject(projectId);
        } else {
            tasks = List.of();
        }

        Map<Long, List<Task>> byAssignee = tasks.stream()
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getUserId()));

        List<Map<String, Object>> data = byAssignee.entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    List<Task> userTasks = entry.getValue();
                    User u = userTasks.get(0).getAssignedTo();
                    int assigned = userTasks.size();
                    int completed = (int) userTasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
                    int efficiency = assigned > 0 ? (int) ((double) completed / assigned * 100) : 0;

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("user_id", userId);
                    m.put("full_name", u.getFullName());
                    m.put("role", u.getRole() != null ? u.getRole().getRoleName() : null);
                    m.put("assigned", assigned);
                    m.put("completed", completed);
                    m.put("efficiency_percent", efficiency);
                    m.put("trend", efficiency >= 80 ? "UP" : efficiency >= 50 ? "FLAT" : "DOWN");
                    m.put("trend_delta_pct", 0);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("data", data));
    }

    @GetMapping("/milestones")
    public ResponseEntity<?> getMilestones(@RequestParam("project_id") Long projectId) {
        projectService.findById(projectId)
                .orElseThrow(() -> new com.ociproject.exception.ResourceNotFoundException("Project not found."));

        List<ProjectSprint> sprints = projectService.findSprints(projectId);
        List<Map<String, Object>> milestones = sprints.stream()
                .sorted(Comparator.comparingInt(ProjectSprint::getSprintNumber))
                .map(ps -> {
                    Sprint s = ps.getSprint();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", s.getName());
                    m.put("date", s.getEndDate() != null ? s.getEndDate().toString() : null);
                    m.put("status", s.getStatus() != null ? s.getStatus().name() : "PENDING");
                    m.put("is_current", s.getStatus() == Sprint.Status.ACTIVE);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "project_id", projectId,
                "milestones", milestones
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPdf(@RequestParam("sprint_id") Long sprintId) {
        Sprint sprint = sprintService.findById(sprintId)
                .orElseThrow(() -> new com.ociproject.exception.ResourceNotFoundException("Sprint not found."));

        // Placeholder PDF generation — returns minimal PDF bytes
        byte[] pdfContent = ("%PDF-1.4 Sprint Report: " + sprint.getName()).getBytes();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"sprint_" + sprintId + "_report.pdf\"")
                .body(pdfContent);
    }
}
