package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LLM_ANALYSIS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class LlmAnalysis {

    public enum ScopeType { USER, PROJECT, SPRINT, GLOBAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_ID")
    private Long analysisId;

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

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "ANOMALY_DETECTED", length = 1)
    private Boolean anomalyDetected;

    @Column(name = "ANOMALY_TYPE", length = 100)
    private String anomalyType;

    @Column(name = "CONFIDENCE_SCORE", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Lob
    @Column(name = "RECOMMENDATION")
    private String recommendation;

    @Column(name = "ANALYSIS_DATE", updatable = false)
    private LocalDateTime analysisDate;

    @PrePersist
    protected void onCreate() {
        analysisDate = LocalDateTime.now();
    }
}
