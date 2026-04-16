package com.ociproject.controller;

import com.ociproject.dto.request.TimeTrackingRequest;
import com.ociproject.model.*;
import com.ociproject.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Dashboard", description = "User dashboard, notifications, and time tracking")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final TaskService taskService;
    private final SprintService sprintService;
    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal User currentUser) {
        // User info
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("user_id", currentUser.getUserId());
        user.put("full_name", currentUser.getFullName());
        user.put("role", currentUser.getRole() != null ? currentUser.getRole().getRoleName() : null);
        user.put("team_name", currentUser.getTeam() != null ? currentUser.getTeam().getName() : null);

        // My tasks
        List<Task> myTasks = taskService.findByAssignee(currentUser.getUserId());
        List<Task> activeTasks = myTasks.stream()
                .filter(t -> t.getStatus() != Task.Status.DONE && t.getStatus() != Task.Status.CANCELLED)
                .sorted(Comparator.comparing(t -> t.getDueDate() != null ? t.getDueDate() : LocalDate.MAX))
                .limit(5)
                .collect(Collectors.toList());

        long highPriority = myTasks.stream().filter(t -> t.getPriority() == Task.Priority.HIGH
                && t.getStatus() != Task.Status.DONE).count();

        // Greeting summary
        Map<String, Object> greetingSummary = new LinkedHashMap<>();
        greetingSummary.put("high_priority_task_count", highPriority);
        greetingSummary.put("pending_review_count", myTasks.stream()
                .filter(t -> t.getStatus() == Task.Status.PENDING).count());

        // Current sprint (find active)
        List<Sprint> activeSprints = sprintService.findByStatus(Sprint.Status.ACTIVE);
        Map<String, Object> currentSprint = new LinkedHashMap<>();
        if (!activeSprints.isEmpty()) {
            Sprint sprint = activeSprints.get(0);
            List<Task> sprintTasks = taskService.findBySprint(sprint.getSprintId());
            int total = sprintTasks.size();
            int completed = (int) sprintTasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
            long daysLeft = sprint.getEndDate() != null
                    ? Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), sprint.getEndDate()))
                    : 0;
            int velocity = total > 0 ? (int) ((double) completed / total * 100) : 0;

            currentSprint.put("sprint_id", sprint.getSprintId());
            currentSprint.put("name", sprint.getName());
            currentSprint.put("status", sprint.getStatus().name());
            currentSprint.put("days_left", daysLeft);
            currentSprint.put("velocity_percent", velocity);
            currentSprint.put("completed_story_points", completed);
            currentSprint.put("total_story_points", total);
            currentSprint.put("on_track", velocity >= 50);

            greetingSummary.put("current_sprint_name", sprint.getName());
        }

        // My tasks next 5
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> myTasksNext5 = activeTasks.stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("task_id", t.getTaskId());
                    m.put("title", t.getTitle());
                    m.put("project_name", t.getProject() != null ? t.getProject().getName() : "");
                    m.put("status", t.getStatus() != null ? t.getStatus().name() : "");
                    m.put("priority", t.getPriority() != null ? t.getPriority().name() : "");
                    m.put("due_date", t.getDueDate() != null ? t.getDueDate().toString() : null);
                    m.put("is_today", t.getDueDate() != null && t.getDueDate().equals(today));
                    m.put("is_tomorrow", t.getDueDate() != null && t.getDueDate().equals(today.plusDays(1)));
                    m.put("is_overdue", t.getDueDate() != null && t.getDueDate().isBefore(today));
                    return m;
                })
                .collect(Collectors.toList());

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user);
        response.put("greeting_summary", greetingSummary);
        response.put("current_sprint", currentSprint);
        response.put("my_tasks_next5", myTasksNext5);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/clear")
    public ResponseEntity<?> clearNotifications() {
        return ResponseEntity.ok(Map.of("message", "All notifications cleared.", "cleared_count", 0));
    }

    @GetMapping("/time-tracking")
    public ResponseEntity<?> getTimeTracking(@AuthenticationPrincipal User currentUser) {
        List<Task> inProgress = taskService.findByAssignee(currentUser.getUserId()).stream()
                .filter(t -> t.getStatus() == Task.Status.IN_PROGRESS)
                .limit(1)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user_id", currentUser.getUserId());
        if (!inProgress.isEmpty()) {
            Task task = inProgress.get(0);
            Map<String, Object> activeTask = new LinkedHashMap<>();
            activeTask.put("task_id", task.getTaskId());
            activeTask.put("title", task.getTitle());
            activeTask.put("project_name", task.getProject() != null ? task.getProject().getName() : "");
            activeTask.put("sprint_name", task.getSprint() != null ? task.getSprint().getName() : "");
            response.put("active_task", activeTask);
            response.put("session_seconds", 0);
            response.put("started_at", LocalDateTime.now().toString());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/time-tracking/pause")
    public ResponseEntity<?> pauseTimeTracking(@RequestBody TimeTrackingRequest request) {
        return ResponseEntity.ok(Map.of(
                "task_id", request.getTaskId(),
                "session_seconds", 0,
                "paused_at", LocalDateTime.now().toString(),
                "status", "PAUSED"
        ));
    }

    @PostMapping("/time-tracking/complete")
    public ResponseEntity<?> completeTimeTracking(@RequestBody TimeTrackingRequest request) {
        return ResponseEntity.ok(Map.of(
                "task_id", request.getTaskId(),
                "total_session_seconds", 0,
                "completed_at", LocalDateTime.now().toString(),
                "status", "COMPLETED"
        ));
    }
}
