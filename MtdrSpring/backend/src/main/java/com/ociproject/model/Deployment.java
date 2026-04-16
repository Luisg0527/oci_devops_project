package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DEPLOYMENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class Deployment {

    public enum Environment { DEV, QA, STAGING, PRODUCTION }
    public enum Status      { SUCCESS, FAILED, IN_PROGRESS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEPLOYMENT_ID")
    private Long deploymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJECT_ID")
    private Project project;

    @Column(name = "VERSION", length = 50)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "ENVIRONMENT", length = 50)
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private Status status;

    @Column(name = "DEPLOYED_AT", updatable = false)
    private LocalDateTime deployedAt;

    @Column(name = "RECOVERY_TIME_MIN", precision = 10, scale = 2)
    private BigDecimal recoveryTimeMin;

    @PrePersist
    protected void onCreate() {
        deployedAt = LocalDateTime.now();
    }
}
