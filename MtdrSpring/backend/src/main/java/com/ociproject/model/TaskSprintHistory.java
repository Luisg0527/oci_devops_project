package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TASK_SPRINT_HISTORY")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class TaskSprintHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HISTORY_ID")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TASK_ID", nullable = false)
    private Task task;

    // NULL when the task is assigned to a sprint for the first time
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OLD_SPRINT_ID")
    private Sprint oldSprint;

    // NULL when the task is removed from a sprint
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NEW_SPRINT_ID")
    private Sprint newSprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHANGED_BY")
    private User changedBy;

    @Column(name = "CHANGED_AT", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
