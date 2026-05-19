package com.ociproject.service.embedding;

import com.ociproject.model.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produces the natural-language text used as the embedding input for each
 * entity, plus the payload metadata stored alongside the vector in Qdrant
 * (used for ACL/filtering and for hydration without going back to the DB).
 *
 * Keep these strings tight: longer text → more tokens to embed → slower and
 * fuzzier search. Aim for a single rich sentence per entity.
 */
@Component
public class CanonicalTextBuilder {

    public String text(Project p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Proyecto \"").append(safe(p.getName())).append("\"");
        if (p.getStatus() != null) sb.append(" [estado: ").append(p.getStatus()).append("]");
        if (p.getManager() != null && p.getManager().getFullName() != null) {
            sb.append(" — manager: ").append(p.getManager().getFullName());
        }
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            sb.append(". Descripción: ").append(truncate(p.getDescription(), 1500));
        }
        return sb.toString();
    }

    public String text(Sprint s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sprint \"").append(safe(s.getName())).append("\"");
        if (s.getStatus() != null) sb.append(" [").append(s.getStatus()).append("]");
        if (s.getStartDate() != null) sb.append(" del ").append(s.getStartDate());
        if (s.getEndDate() != null) sb.append(" al ").append(s.getEndDate());
        if (s.getTotalHours() != null) sb.append(" — horas totales: ").append(s.getTotalHours());
        return sb.toString();
    }

    public String text(Task t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tarea \"").append(safe(t.getTitle())).append("\"");
        if (t.getStatus() != null) sb.append(" [").append(t.getStatus()).append("]");
        if (t.getPriority() != null) sb.append(" prioridad ").append(t.getPriority());
        if (t.getProject() != null && t.getProject().getName() != null) {
            sb.append(" en proyecto \"").append(t.getProject().getName()).append("\"");
        }
        if (t.getSprint() != null && t.getSprint().getName() != null) {
            sb.append(" sprint \"").append(t.getSprint().getName()).append("\"");
        }
        if (t.getAssignedTo() != null && t.getAssignedTo().getFullName() != null) {
            sb.append(" asignada a ").append(t.getAssignedTo().getFullName());
        }
        if (t.getDueDate() != null) sb.append(", vence ").append(t.getDueDate());
        if (t.getEstimatedHours() != null) sb.append(", estimadas ").append(t.getEstimatedHours()).append("h");
        if (t.getActualHours() != null) sb.append(", reales ").append(t.getActualHours()).append("h");
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            sb.append(". Detalle: ").append(truncate(t.getDescription(), 1500));
        }
        return sb.toString();
    }

    public String text(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usuario ").append(safe(u.getFullName()));
        if (u.getRole() != null && u.getRole().getRoleName() != null) {
            sb.append(" — rol: ").append(u.getRole().getRoleName());
        }
        if (u.getTeam() != null && u.getTeam().getName() != null) {
            sb.append(" — equipo: ").append(u.getTeam().getName());
        }
        if (u.getStatus() != null) sb.append(" [").append(u.getStatus()).append("]");
        return sb.toString();
    }

    public String text(Team t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Equipo \"").append(safe(t.getName())).append("\"");
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            sb.append(". ").append(truncate(t.getDescription(), 500));
        }
        return sb.toString();
    }

    public String text(KpiValue k) {
        StringBuilder sb = new StringBuilder();
        sb.append("KPI ");
        if (k.getKpiType() != null && k.getKpiType().getName() != null) {
            sb.append("\"").append(k.getKpiType().getName()).append("\"");
        }
        if (k.getScopeType() != null) sb.append(" (").append(k.getScopeType()).append(")");
        if (k.getValue() != null) sb.append(" = ").append(k.getValue());
        if (k.getProject() != null && k.getProject().getName() != null) {
            sb.append(", proyecto \"").append(k.getProject().getName()).append("\"");
        }
        if (k.getSprint() != null && k.getSprint().getName() != null) {
            sb.append(", sprint \"").append(k.getSprint().getName()).append("\"");
        }
        if (k.getUser() != null && k.getUser().getFullName() != null) {
            sb.append(", usuario ").append(k.getUser().getFullName());
        }
        if (k.getRecordedAt() != null) sb.append(" — ").append(k.getRecordedAt().toLocalDate());
        return sb.toString();
    }

    public String text(Deployment d) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deployment ");
        if (d.getVersion() != null) sb.append("v").append(d.getVersion());
        if (d.getEnvironment() != null) sb.append(" a ").append(d.getEnvironment());
        if (d.getStatus() != null) sb.append(" [").append(d.getStatus()).append("]");
        if (d.getProject() != null && d.getProject().getName() != null) {
            sb.append(" del proyecto \"").append(d.getProject().getName()).append("\"");
        }
        if (d.getDeployedAt() != null) sb.append(" — ").append(d.getDeployedAt().toLocalDate());
        if (d.getRecoveryTimeMin() != null) sb.append(", recovery ").append(d.getRecoveryTimeMin()).append(" min");
        return sb.toString();
    }

    public String text(Incident i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Incidente");
        if (i.getType() != null) sb.append(" tipo \"").append(i.getType()).append("\"");
        if (i.getSeverity() != null) sb.append(" severidad ").append(i.getSeverity());
        if (i.getProject() != null && i.getProject().getName() != null) {
            sb.append(" en proyecto \"").append(i.getProject().getName()).append("\"");
        }
        if (i.getOccurredAt() != null) sb.append(", ocurrió ").append(i.getOccurredAt().toLocalDate());
        if (i.getResolvedAt() != null) sb.append(", resuelto ").append(i.getResolvedAt().toLocalDate());
        if (i.getDescription() != null && !i.getDescription().isBlank()) {
            sb.append(". ").append(truncate(i.getDescription(), 1500));
        }
        return sb.toString();
    }

    /** Payload stored alongside the vector. Used for filtering and for hydrating
     *  search hits without an extra DB round-trip. */
    public Map<String, Object> payload(Project p) {
        Map<String, Object> m = baseMeta();
        if (p.getProjectId() != null) m.put("project_id", p.getProjectId());
        if (p.getStatus() != null) m.put("status", p.getStatus().name());
        return m;
    }
    public Map<String, Object> payload(Sprint s) {
        Map<String, Object> m = baseMeta();
        if (s.getSprintId() != null) m.put("sprint_id", s.getSprintId());
        if (s.getStatus() != null) m.put("status", s.getStatus().name());
        return m;
    }
    public Map<String, Object> payload(Task t) {
        Map<String, Object> m = baseMeta();
        if (t.getProject() != null) m.put("project_id", t.getProject().getProjectId());
        if (t.getSprint() != null) m.put("sprint_id", t.getSprint().getSprintId());
        if (t.getAssignedTo() != null) m.put("assignee_id", t.getAssignedTo().getUserId());
        if (t.getStatus() != null) m.put("status", t.getStatus().name());
        if (t.getPriority() != null) m.put("priority", t.getPriority().name());
        return m;
    }
    public Map<String, Object> payload(User u) {
        Map<String, Object> m = baseMeta();
        if (u.getTeam() != null) m.put("team_id", u.getTeam().getTeamId());
        if (u.getStatus() != null) m.put("status", u.getStatus().name());
        return m;
    }
    public Map<String, Object> payload(Team t) {
        return baseMeta();
    }
    public Map<String, Object> payload(KpiValue k) {
        Map<String, Object> m = baseMeta();
        if (k.getProject() != null) m.put("project_id", k.getProject().getProjectId());
        if (k.getSprint() != null) m.put("sprint_id", k.getSprint().getSprintId());
        if (k.getUser() != null) m.put("user_id", k.getUser().getUserId());
        if (k.getScopeType() != null) m.put("scope", k.getScopeType().name());
        return m;
    }
    public Map<String, Object> payload(Deployment d) {
        Map<String, Object> m = baseMeta();
        if (d.getProject() != null) m.put("project_id", d.getProject().getProjectId());
        if (d.getEnvironment() != null) m.put("environment", d.getEnvironment().name());
        if (d.getStatus() != null) m.put("status", d.getStatus().name());
        return m;
    }
    public Map<String, Object> payload(Incident i) {
        Map<String, Object> m = baseMeta();
        if (i.getProject() != null) m.put("project_id", i.getProject().getProjectId());
        if (i.getSeverity() != null) m.put("severity", i.getSeverity().name());
        return m;
    }

    private static Map<String, Object> baseMeta() {
        return new LinkedHashMap<>();
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
