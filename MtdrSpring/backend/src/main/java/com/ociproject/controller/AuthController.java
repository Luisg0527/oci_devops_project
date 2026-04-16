package com.ociproject.controller;

import com.ociproject.dto.request.LoginRequest;
import com.ociproject.dto.request.RefreshTokenRequest;
import com.ociproject.dto.response.LoginResponse;
import com.ociproject.dto.response.TokenResponse;
import com.ociproject.model.User;
import com.ociproject.model.UserCredential;
import com.ociproject.security.JwtTokenProvider;
import com.ociproject.service.AuditLogService;
import com.ociproject.service.UserCredentialService;
import com.ociproject.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Tag(name = "Auth", description = "Login, logout, and token refresh")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserCredentialService credentialService;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        UserCredential credential = credentialService.findByUsername(request.getUsername())
                .orElse(null);

        if (credential == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "failed_attempts", 0,
                    "account_locked", false
            ));
        }

        if (Boolean.TRUE.equals(credential.getAccountLocked())) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Account is locked",
                    "failed_attempts", credential.getFailedAttempts(),
                    "account_locked", true
            ));
        }

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            credentialService.recordFailedLogin(credential.getUser().getUserId());
            UserCredential updated = credentialService.findByUsername(request.getUsername()).orElse(credential);
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "failed_attempts", updated.getFailedAttempts(),
                    "account_locked", Boolean.TRUE.equals(updated.getAccountLocked())
            ));
        }

        User user = credential.getUser();
        credentialService.recordSuccessfulLogin(user.getUserId());

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "USER";
        String token = tokenProvider.generateToken(user.getUserId(), credential.getUsername(), roleName);
        String refreshToken = tokenProvider.generateRefreshToken(user.getUserId());
        String expiresAt = DateTimeFormatter.ISO_INSTANT.format(
                tokenProvider.getExpirationFromToken(token).toInstant());

        auditLogService.log(user, "LOGIN_ATTEMPT", "USER_CREDENTIALS",
                credential.getCredentialId(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleName)
                .teamId(user.getTeam() != null ? user.getTeam().getTeamId() : null)
                .expiresAt(expiresAt)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("message", "Session terminated successfully."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "USER";
        UserCredential credential = credentialService.findByUserId(userId).orElse(null);
        String username = credential != null ? credential.getUsername() : "";
        String newToken = tokenProvider.generateToken(userId, username, roleName);
        String expiresAt = DateTimeFormatter.ISO_INSTANT.format(
                tokenProvider.getExpirationFromToken(newToken).toInstant());

        return ResponseEntity.ok(TokenResponse.builder()
                .token(newToken)
                .expiresAt(expiresAt)
                .build());
    }
}
