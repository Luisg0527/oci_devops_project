package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROJECTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class Project {

    public enum Status {
        ACTIVE, INACTIVE, CLOSED;

        /** Valores permitidos por la BD; acepta alias legacy del API/UI. */
        public static Status fromApiValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return ACTIVE;
            }
            return switch (raw.trim().toUpperCase()) {
                case "ACTIVE" -> ACTIVE;
                case "INACTIVE", "ON_HOLD" -> INACTIVE;
                case "CLOSED", "COMPLETED", "CANCELLED" -> CLOSED;
                default -> throw new IllegalArgumentException("Estado de proyecto no válido: " + raw);
            };
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROJECT_ID")
    private Long projectId;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_ID", nullable = false)
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "IS_DELETED", length = 1)
    private Boolean deleted = false;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    /** Maintained automatically by DB trigger TRG_TASK_HOURS_ROLLUP. */
    @Column(name = "TOTAL_HOURS", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalHours;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
