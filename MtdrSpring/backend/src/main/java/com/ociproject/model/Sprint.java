package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SPRINTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class Sprint {

    public enum Status { PLANNED, ACTIVE, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SPRINT_ID")
    private Long sprintId;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private Status status = Status.PLANNED;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "IS_DELETED", length = 1)
    private Boolean deleted = false;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
