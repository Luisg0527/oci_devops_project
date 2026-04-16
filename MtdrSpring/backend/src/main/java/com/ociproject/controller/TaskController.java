package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.CreateTaskRequest;
import com.ociproject.dto.request.UpdateTaskRequest;
import com.ociproject.dto.response.TaskResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Tasks", description = "Task CRUD, status/sprint history")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final SprintService sprintService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "project_id", required = false) Long projectId,
            @RequestParam(name = "sprint_id", required = false) Long sprintId,
            @RequestParam(name = "assigned_to", required = false) Long assignedTo,
            @RequestParam(name = "task_stage", required = false) String taskStage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(name = "due_before", required = false) LocalDate dueBefore,
            @RequestParam(name = "due_after", required = false) LocalDate dueAfter,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<Task> tasks;
        if (projectId != null && taskStage != null) {
            tasks = taskService.findByProjectAndStage(projectId, Task.Stage.valueOf(taskStage));
        } else if (projectId != null && status != null) {
            tasks = taskService.findByProjectAndStatus(projectId, Task.Status.valueOf(status));
        } else if (projectId != null) {
            tasks = taskService.findByProject(projectId);
        } else if (sprintId != null) {
            tasks = taskService.findBySprint(sprintId);
        } else if (assignedTo != null) {
            tasks = taskService.findByAssignee(assignedTo);
        } else {
            tasks = taskService.findByProject(projectId != null ? projectId : 0L);
            if (tasks.isEmpty()) {
                tasks = taskService.findByAssignee(0L);
            }
        }

        if (priority != null) {
            Task.Priority p = Task.Priority.valueOf(priority);
            tasks = tasks.stream().filter(t -> t.getPriority() == p).collect(Collectors.toList());
        }
        if (dueBefore != null) {
            tasks = tasks.stream().filter(t -> t.getDueDate() != null && !t.getDueDate().isAfter(dueBefore))
                    .collect(Collectors.toList());
        }
        if (dueAfter != null) {
            tasks = tasks.stream().filter(t -> t.getDueDate() != null && !t.getDueDate().isBefore(dueAfter))
                    .collect(Collectors.toList());
        }

        List<TaskResponse> data = tasks.stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<TaskResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<TaskResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long taskId) {
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        TaskResponse response = TaskResponse.from(task);
        response.setStatusHistory(taskService.findStatusHistory(taskId).stream()
                .map(h -> TaskResponse.StatusHistoryEntry.builder()
                        .historyId(h.getHistoryId())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUserId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList()));
        response.setSprintHistory(taskService.findSprintHistory(taskId).stream()
                .map(h -> TaskResponse.SprintHistoryEntry.builder()
                        .historyId(h.getHistoryId())
                        .oldSprintId(h.getOldSprint() != null ? h.getOldSprint().getSprintId() : null)
                        .oldSprintName(h.getOldSprint() != null ? h.getOldSprint().getName() : null)
                        .newSprintId(h.getNewSprint() != null ? h.getNewSprint().getSprintId() : null)
                        .newSprintName(h.getNewSprint() != null ? h.getNewSprint().getName() : null)
                        .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUserId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyTasks(@AuthenticationPrincipal User currentUser,
                                        @RequestParam(defaultValue = "5") int limit,
                                        @RequestParam(name = "sprint_id", required = false) Long sprintId) {
        List<Task> tasks = taskService.findByAssignee(currentUser.getUserId());
        if (sprintId != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getSprint() != null && t.getSprint().getSprintId().equals(sprintId))
                    .collect(Collectors.toList());
        }

        tasks = tasks.stream()
                .filter(t -> t.getStatus() != Task.Status.DONE && t.getStatus() != Task.Status.CANCELLED)
                .sorted((a, b) -> {
                    if (a.getDueDate() == null && b.getDueDate() == null) return 0;
                    if (a.getDueDate() == null) return 1;
                    if (b.getDueDate() == null) return -1;
                    return a.getDueDate().compareTo(b.getDueDate());
                })
                .limit(limit)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> taskList = tasks.stream()
                .map(t -> {
                    boolean isToday = t.getDueDate() != null && t.getDueDate().equals(today);
                    boolean isTomorrow = t.getDueDate() != null && t.getDueDate().equals(today.plusDays(1));
                    boolean isOverdue = t.getDueDate() != null && t.getDueDate().isBefore(today);
                    return Map.<String, Object>of(
                            "task_id", t.getTaskId(),
                            "title", t.getTitle(),
                            "project_name", t.getProject() != null ? t.getProject().getName() : "",
                            "sprint_name", t.getSprint() != null ? t.getSprint().getName() : "",
                            "status", t.getStatus() != null ? t.getStatus().name() : "",
                            "priority", t.getPriority() != null ? t.getPriority().name() : "",
                            "due_date", t.getDueDate() != null ? t.getDueDate().toString() : "",
                            "is_today", isToday,
                            "is_tomorrow", isTomorrow,
                            "is_overdue", isOverdue
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "user_id", currentUser.getUserId(),
                "user_name", currentUser.getFullName(),
                "tasks", taskList
        ));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateTaskRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Project project = projectService.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        Sprint sprint = null;
        if (request.getSprintId() != null) {
            sprint = sprintService.findById(request.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found."));
        }

        User creator = userService.findById(request.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Creator user not found."));
        User assignee = null;
        if (request.getAssignedTo() != null) {
            assignee = userService.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found."));
        }

        Task task = Task.builder()
                .project(project)
                .sprint(sprint)
                .title(request.getTitle())
                .description(request.getDescription())
                .taskStage(request.getTaskStage() != null
                        ? Task.Stage.valueOf(request.getTaskStage())
                        : (sprint != null ? Task.Stage.SPRINT : Task.Stage.BACKLOG))
                .status(request.getStatus() != null
                        ? Task.Status.valueOf(request.getStatus())
                        : Task.Status.PENDING)
                .priority(request.getPriority() != null
                        ? Task.Priority.valueOf(request.getPriority())
                        : null)
                .createdBy(creator)
                .assignedTo(assignee)
                .dueDate(request.getDueDate())
                .deleted(false)
                .build();
        task = taskService.save(task);
        auditLogService.log(actor, "CREATE", "TASKS", task.getTaskId(), httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> update(@PathVariable Long taskId,
                                    @RequestBody UpdateTaskRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Task task = taskService.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(Task.Priority.valueOf(request.getPriority()));
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getTaskStage() != null) task.setTaskStage(Task.Stage.valueOf(request.getTaskStage()));

        if (request.getAssignedTo() != null) {
            User assignee = userService.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found."));
            task.setAssignedTo(assignee);
        }

        // Side effect: status change → auto-insert TaskStatusHistory
        if (request.getStatus() != null) {
            Task.Status newStatus = Task.Status.valueOf(request.getStatus());
            if (task.getStatus() != newStatus) {
                task = taskService.updateStatus(taskId, newStatus, actor);
            }
        }

        // Side effect: sprint change → auto-insert TaskSprintHistory
        if (request.getSprintId() != null) {
            Long currentSprintId = task.getSprint() != null ? task.getSprint().getSprintId() : null;
            if (!request.getSprintId().equals(currentSprintId)) {
                Sprint newSprint = sprintService.findById(request.getSprintId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sprint not found."));
                task = taskService.assignToSprint(taskId, newSprint, actor);
            }
        }

        task = taskService.save(task);
        auditLogService.log(actor, "UPDATE", "TASKS", taskId, httpRequest.getRemoteAddr());

        // Return full detail with history
        TaskResponse response = TaskResponse.from(task);
        response.setStatusHistory(taskService.findStatusHistory(taskId).stream()
                .map(h -> TaskResponse.StatusHistoryEntry.builder()
                        .historyId(h.getHistoryId())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUserId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> delete(@PathVariable Long taskId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        taskService.softDelete(taskId);
        auditLogService.log(actor, "DELETE", "TASKS", taskId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Task soft-deleted successfully.",
                "task_id", taskId,
                "deleted_at", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{taskId}/status-history")
    public ResponseEntity<?> getStatusHistory(@PathVariable Long taskId) {
        List<TaskResponse.StatusHistoryEntry> history = taskService.findStatusHistory(taskId).stream()
                .map(h -> TaskResponse.StatusHistoryEntry.builder()
                        .historyId(h.getHistoryId())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUserId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("task_id", taskId, "history", history));
    }

    @GetMapping("/{taskId}/sprint-history")
    public ResponseEntity<?> getSprintHistory(@PathVariable Long taskId) {
        List<TaskResponse.SprintHistoryEntry> history = taskService.findSprintHistory(taskId).stream()
                .map(h -> TaskResponse.SprintHistoryEntry.builder()
                        .historyId(h.getHistoryId())
                        .oldSprintId(h.getOldSprint() != null ? h.getOldSprint().getSprintId() : null)
                        .oldSprintName(h.getOldSprint() != null ? h.getOldSprint().getName() : null)
                        .newSprintId(h.getNewSprint() != null ? h.getNewSprint().getSprintId() : null)
                        .newSprintName(h.getNewSprint() != null ? h.getNewSprint().getName() : null)
                        .changedBy(h.getChangedBy() != null ? h.getChangedBy().getUserId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("task_id", taskId, "history", history));
    }
}
