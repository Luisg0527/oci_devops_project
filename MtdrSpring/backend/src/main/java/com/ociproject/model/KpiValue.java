package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "KPI_VALUES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class KpiValue {

    public enum ScopeType { USER, PROJECT, SPRINT, GLOBAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KPI_VALUE_ID")
    private Long kpiValueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KPI_TYPE_ID", nullable = false)
    private KpiType kpiType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SCOPE_TYPE", nullable = false, length = 20)
    private ScopeType scopeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SPRINT_ID")
    private Sprint sprint;

    @Column(name = "VALUE", nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(name = "RECORDED_AT", updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
