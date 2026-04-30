package com.ociproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ociproject.config.GroqProperties;
import com.ociproject.model.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a curated JSON snapshot of the org's data for the AI assistant.
 * Reuses existing services so no query logic is duplicated.
 *
 * Important: this method is @Transactional(readOnly=true) so all lazy
 * relations (User.role, User.team, Project.manager, Task.project, etc.)
 * can be navigated within a single Hibernate session.
 */
@Service
@RequiredArgsConstructor
public class AiContextService {

    private static final Logger log = LoggerFactory.getLogger(AiContextService.class);
    private static final int CLOSED_SPRINTS_DAYS_BACK = 90;
    private static final int RECENT_TASKS_LIMIT_PER_SPRINT = 60;

    private final ProjectService projectService;
    private final SprintService sprintService;
    private final TaskService taskService;
    private final UserService userService;
    private final TeamService teamService;
    private final KpiService kpiService;
    private final ObjectMapper objectMapper;
    private final GroqProperties groqProps;

    /**
     * Builds a JSON string with the curated organizational snapshot.
     * Caps total size to {@code groq.max-context-chars} to avoid token blowups.
     */
    @Transactional(readOnly = true)
    public String buildSnapshotJson() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("generated_at", LocalDateTime.now().toString());
        snapshot.put("today", LocalDate.now().toString());

        snapshot.put("projects", buildProjects());
        snapshot.put("sprints", buildSprints());
        snapshot.put("tasks", buildTasks());
        snapshot.put("users", buildUsers());
        snapshot.put("teams", buildTeams());
        snapshot.put("kpis_recent", buildRecentKpis());

        try {
            String json = objectMapper.writeValueAsString(snapshot);
            int max = groqProps.getMaxContextChars();
            if (json.length() > max) {
                log.warn("Snapshot ({} chars) exceeds cap ({}); truncating.", json.length(), max);
                json = json.substring(0, max) + "\"... [truncated]\"";
            }
            return json;
        } catch (JsonProcessingException e) {
            log.error("No se pudo serializar el snapshot: {}", e.getMessage(), e);
            return "{}";
        }
    }

    private List<Map<String, Object>> buildProjects() {
        return projectService.findAll().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getProjectId());
                    m.put("name", p.getName());
                    m.put("description", p.getDescription());
                    m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
                    m.put("manager", p.getManager() != null ? p.getManager().getFullName() : null);
                    m.put("total_hours", p.getTotalHours());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildSprints() {
        LocalDate cutoff = LocalDate.now().minusDays(CLOSED_SPRINTS_DAYS_BACK);
        return sprintService.findAll().stream()
                .filter(s -> s.getStatus() != Sprint.Status.CLOSED
                        || (s.getEndDate() != null && !s.getEndDate().isBefore(cutoff)))
                .sorted(Comparator.comparing(Sprint::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getSprintId());
                    m.put("name", s.getName());
                    m.put("status", s.getStatus() != null ? s.getStatus().name() : null);
                    m.put("start_date", s.getStartDate());
                    m.put("end_date", s.getEndDate());
                    m.put("total_hours", s.getTotalHours());
                    if (s.getStatus() == Sprint.Status.ACTIVE && s.getEndDate() != null) {
                        m.put("days_left", Math.max(0,
                                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate())));
                    }
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTasks() {
        // Group recent tasks by sprint. Include all tasks in active sprints,
        // and a capped sample of tasks from recent closed sprints.
        LocalDate cutoff = LocalDate.now().minusDays(CLOSED_SPRINTS_DAYS_BACK);
        List<Sprint> relevantSprints = sprintService.findAll().stream()
                .filter(s -> s.getStatus() != Sprint.Status.CLOSED
                        || (s.getEndDate() != null && !s.getEndDate().isBefore(cutoff)))
                .toList();

        List<Map<String, Object>> allTasks = new ArrayList<>();

        for (Sprint s : relevantSprints) {
            List<Task> tasks = taskService.findBySprint(s.getSprintId());
            int limit = s.getStatus() == Sprint.Status.ACTIVE
                    ? Integer.MAX_VALUE
                    : RECENT_TASKS_LIMIT_PER_SPRINT;
            tasks.stream().limit(limit).forEach(t -> allTasks.add(taskToMap(t)));
        }

        // Also include backlog tasks (no sprint), capped.
        taskService.findAll().stream()
                .filter(t -> t.getSprint() == null)
                .filter(t -> t.getTaskStage() == Task.Stage.BACKLOG)
                .limit(50)
                .forEach(t -> allTasks.add(taskToMap(t)));

        return allTasks;
    }

    private Map<String, Object> taskToMap(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getTaskId());
        m.put("title", t.getTitle());
        m.put("project_id", t.getProject() != null ? t.getProject().getProjectId() : null);
        m.put("project_name", t.getProject() != null ? t.getProject().getName() : null);
        m.put("sprint_id", t.getSprint() != null ? t.getSprint().getSprintId() : null);
        m.put("sprint_name", t.getSprint() != null ? t.getSprint().getName() : null);
        m.put("stage", t.getTaskStage() != null ? t.getTaskStage().name() : null);
        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        m.put("priority", t.getPriority() != null ? t.getPriority().name() : null);
        m.put("assigned_to", t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null);
        m.put("created_by", t.getCreatedBy() != null ? t.getCreatedBy().getFullName() : null);
        m.put("due_date", t.getDueDate());
        m.put("estimated_hours", t.getEstimatedHours());
        m.put("actual_hours", t.getActualHours());
        m.put("is_overdue", t.getDueDate() != null
                && t.getDueDate().isBefore(LocalDate.now())
                && t.getStatus() != Task.Status.DONE
                && t.getStatus() != Task.Status.CANCELLED);
        return m;
    }

    private List<Map<String, Object>> buildUsers() {
        return userService.findAll().stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getUserId());
                    m.put("name", u.getFullName());
                    m.put("email", u.getEmail());
                    m.put("role", u.getRole() != null ? u.getRole().getRoleName() : null);
                    m.put("team", u.getTeam() != null ? u.getTeam().getName() : null);
                    m.put("status", u.getStatus() != null ? u.getStatus().name() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTeams() {
        return teamService.findAll().stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getTeamId());
                    m.put("name", t.getName());
                    m.put("description", t.getDescription());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentKpis() {
        // Latest 30 KPI values across all scopes; useful for trend questions.
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return kpiService.findValuesByScope(KpiValue.ScopeType.SPRINT).stream()
                .filter(k -> k.getRecordedAt() != null && k.getRecordedAt().isAfter(since))
                .sorted(Comparator.comparing(KpiValue::getRecordedAt, Comparator.reverseOrder()))
                .limit(30)
                .map(this::kpiToMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> kpiToMap(KpiValue k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kpi_type", k.getKpiType() != null ? k.getKpiType().getName() : null);
        m.put("scope", k.getScopeType() != null ? k.getScopeType().name() : null);
        m.put("value", k.getValue() != null ? k.getValue() : BigDecimal.ZERO);
        m.put("project_id", k.getProject() != null ? k.getProject().getProjectId() : null);
        m.put("sprint_id", k.getSprint() != null ? k.getSprint().getSprintId() : null);
        m.put("user_id", k.getUser() != null ? k.getUser().getUserId() : null);
        m.put("recorded_at", k.getRecordedAt());
        return m;
    }
}
