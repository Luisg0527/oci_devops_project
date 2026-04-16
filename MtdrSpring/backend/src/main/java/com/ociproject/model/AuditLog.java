package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOG")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    // ON DELETE SET NULL — may be null after user deletion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "ACTION_TYPE", length = 100)
    private String actionType;

    @Column(name = "ENTITY_NAME", length = 100)
    private String entityName;

    @Column(name = "ENTITY_ID")
    private Long entityId;

    @Column(name = "ACTION_DATE", updatable = false)
    private LocalDateTime actionDate;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        actionDate = LocalDateTime.now();
    }
}
