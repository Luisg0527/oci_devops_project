package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TASK_STATUS_HISTORY")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class TaskStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HISTORY_ID")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TASK_ID", nullable = false)
    private Task task;

    @Column(name = "OLD_STATUS", length = 30)
    private String oldStatus;

    @Column(name = "NEW_STATUS", nullable = false, length = 30)
    private String newStatus;

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
