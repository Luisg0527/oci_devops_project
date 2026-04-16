package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class User {

    public enum Status { ACTIVE, INACTIVE, LOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "FULL_NAME", nullable = false, length = 150)
    private String fullName;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "TELEGRAM_ID", unique = true, length = 100)
    private String telegramId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEAM_ID")
    private Team team;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
