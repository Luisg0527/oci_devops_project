package com.ociproject.model;

import com.ociproject.converter.YnBooleanConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_CREDENTIALS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CREDENTIAL_ID")
    private Long credentialId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private User user;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "PASSWORD_SALT", nullable = false, length = 255)
    private String passwordSalt;

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;

    @Column(name = "FAILED_ATTEMPTS")
    private Integer failedAttempts = 0;

    @Convert(converter = YnBooleanConverter.class)
    @Column(name = "ACCOUNT_LOCKED", length = 1)
    private Boolean accountLocked = false;

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
