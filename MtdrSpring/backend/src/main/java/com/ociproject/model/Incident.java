package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INCIDENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class Incident {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INCIDENT_ID")
    private Long incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID")
    private Project project;

    @Column(name = "TYPE", length = 100)
    private String type;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", length = 20)
    private Severity severity;

    @Column(name = "OCCURRED_AT", updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "IS_DELETED", length = 1)
    private Boolean deleted = false;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        occurredAt = LocalDateTime.now();
    }
}
