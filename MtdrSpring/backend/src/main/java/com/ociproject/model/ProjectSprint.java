package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROJECT_SPRINTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class ProjectSprint {

    @EmbeddedId
    private ProjectSprintId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "PROJECT_ID")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sprintId")
    @JoinColumn(name = "SPRINT_ID")
    private Sprint sprint;

    @Column(name = "SPRINT_NUMBER", nullable = false)
    private Integer sprintNumber;

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "IS_ACTIVE", length = 1)
    private Boolean active = false;

    @Column(name = "ASSIGNED_AT", updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}
