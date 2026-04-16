package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TASKS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class Task {

    public enum Stage    { BACKLOG, SPRINT, COMPLETED }
    public enum Status   { PENDING, IN_PROGRESS, DONE, CANCELLED, REOPENED }
    public enum Priority { LOW, MEDIUM, HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TASK_ID")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPRINT_ID")
    private Sprint sprint;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "TASK_STAGE", length = 20)
    private Stage taskStage = Stage.BACKLOG;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 30)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORITY", length = 20)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNED_TO")
    private User assignedTo;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "IS_DELETED", length = 1)
    private Boolean deleted = false;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
