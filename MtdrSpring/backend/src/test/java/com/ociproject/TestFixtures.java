package com.ociproject;

import com.ociproject.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Canonical test fixtures matching DB/template_data.sql.
 * Each factory method returns a fresh builder instance — no shared mutable state.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ── Roles ──────────────────────────────────────────────────────────────────

    public static Role role1() {
        return Role.builder().roleId(1L).roleName("MANAGER").deleted(false).build();
    }

    public static Role role2() {
        return Role.builder().roleId(2L).roleName("DEVELOPER").deleted(false).build();
    }

    // ── Teams ──────────────────────────────────────────────────────────────────

    public static Team team1() {
        return Team.builder().teamId(1L).name("Backend Team").deleted(false).build();
    }

    public static Team team2() {
        return Team.builder().teamId(2L).name("Frontend Team").deleted(false).build();
    }

    // ── Users ──────────────────────────────────────────────────────────────────

    public static User user1() {
        return User.builder()
                .userId(1L)
                .fullName("Inigo Gonzalez")
                .email("a01723229@tec.mx")
                .telegramId("@inigogonzalez")
                .role(role1())
                .team(team1())
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    public static User user2() {
        return User.builder()
                .userId(2L)
                .fullName("Victor Martinez")
                .email("a01723093@tec.mx")
                .telegramId("@victormartinez")
                .role(role2())
                .team(team1())
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    public static User user3() {
        return User.builder()
                .userId(3L)
                .fullName("Paolo Gaya")
                .email("a01722922@tec.mx")
                .telegramId("@paologaya")
                .role(role2())
                .team(team2())
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    public static User user4() {
        return User.builder()
                .userId(4L)
                .fullName("Miguel Angel Alvarez")
                .email("a01722925@tec.mx")
                .telegramId("@miguelangelalvarez")
                .role(role2())
                .team(team1())
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    public static User user5() {
        return User.builder()
                .userId(5L)
                .fullName("Jinhyuk Park")
                .email("a01286288@tec.mx")
                .telegramId("@jinhyukpark")
                .role(role2())
                .team(team2())
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    public static User user6() {
        return User.builder()
                .userId(6L)
                .fullName("Luis Garza Gomez")
                .email("a00839388@tec.mx")
                .telegramId("@luisgarzagomez")
                .role(role1())
                .team(team1())
                .status(User.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    // ── UserCredential ─────────────────────────────────────────────────────────

    /** Credential for user1 (Inigo Gonzalez). failedAttempts and locked are parameterised
     *  to support the locking/counter tests in UserCredentialServiceTest. */
    public static UserCredential credential1(int failedAttempts, boolean locked) {
        return UserCredential.builder()
                .credentialId(1L)
                .user(user1())
                .username("inigo.gonzalez")
                .passwordHash("b3a8e0e1f9ab1827e1f18c4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4")
                .passwordSalt("SALT_B2C3D4E5F6A1")
                .failedAttempts(failedAttempts)
                .accountLocked(locked)
                .deleted(false)
                .build();
    }

    // ── Sprints ────────────────────────────────────────────────────────────────

    public static Sprint sprint1() {
        return Sprint.builder()
                .sprintId(1L)
                .name("Sprint 1 - Desarrollo base del sistema")
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 1, 24))
                .status(Sprint.Status.CLOSED)
                .deleted(false)
                .build();
    }

    public static Sprint sprint2() {
        return Sprint.builder()
                .sprintId(2L)
                .name("Sprint 2 - Implementacion de KPIs y metricas")
                .startDate(LocalDate.of(2025, 1, 27))
                .endDate(LocalDate.of(2025, 2, 17))
                .status(Sprint.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    // ── Projects ───────────────────────────────────────────────────────────────

    public static Project project1() {
        return Project.builder()
                .projectId(1L)
                .name("Proyecto Alpha")
                .manager(user1())
                .status(Project.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    public static Project project2() {
        return Project.builder()
                .projectId(2L)
                .name("Proyecto Beta")
                .manager(user6())
                .status(Project.Status.ACTIVE)
                .deleted(false)
                .build();
    }

    // ── Tasks ──────────────────────────────────────────────────────────────────

    public static Task pendingTask1() {
        return Task.builder()
                .taskId(1L)
                .title("Definir endpoints de la API")
                .project(project1())
                .sprint(sprint1())
                .taskStage(Task.Stage.BACKLOG)
                .status(Task.Status.PENDING)
                .deleted(false)
                .build();
    }

    // ── KPI Types ──────────────────────────────────────────────────────────────

    public static KpiType kpiType1() {
        return KpiType.builder()
                .kpiTypeId(1L)
                .name("TAREAS_COMPLETADAS_SPRINT")
                .category("DELIVERY")
                .unit("count")
                .build();
    }

    // ── Deployments ────────────────────────────────────────────────────────────

    public static Deployment deployment1() {
        return Deployment.builder()
                .deploymentId(1L)
                .project(project1())
                .version("v1.0.0")
                .environment(Deployment.Environment.STAGING)
                .status(Deployment.Status.IN_PROGRESS)
                .build();
    }

    // ── Incidents ──────────────────────────────────────────────────────────────

    public static Incident incident1() {
        return Incident.builder()
                .incidentId(1L)
                .project(project1())
                .type("PERFORMANCE")
                .severity(Incident.Severity.HIGH)
                .deleted(false)
                .build();
    }

    // ── Bot Interactions ───────────────────────────────────────────────────────

    /** Fixed timestamp avoids LocalDateTime.now() non-determinism in the composite PK. */
    public static BotInteraction botInteraction1() {
        BotInteractionId id = new BotInteractionId(1L, LocalDateTime.of(2025, 2, 5, 9, 15, 0));
        return BotInteraction.builder()
                .id(id)
                .user(user2())
                .message("/tareas pendientes")
                .response("Tienes 2 tareas activas en Sprint 2")
                .build();
    }
}
