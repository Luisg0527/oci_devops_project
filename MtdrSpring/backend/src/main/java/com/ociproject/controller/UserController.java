package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.CreateUserRequest;
import com.ociproject.dto.request.UpdateUserRequest;
import com.ociproject.dto.response.UserResponse;
import com.ociproject.dto.response.WorkloadResponse;
import com.ociproject.exception.ConflictException;
import com.ociproject.exception.ResourceNotFoundException;
import com.ociproject.model.*;
import com.ociproject.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Users", description = "User CRUD and workload metrics")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserCredentialService credentialService;
    private final RoleService roleService;
    private final TeamService teamService;
    private final TaskService taskService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted) {

        List<User> users;
        if (teamId != null) {
            users = userService.findByTeam(teamId);
        } else if (roleId != null) {
            users = userService.findByRole(roleId);
        } else if (status != null) {
            users = userService.findByStatus(User.Status.valueOf(status));
        } else {
            users = userService.findAll();
        }

        List<UserResponse> data = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<UserResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<UserResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email or username already in use.");
        }
        if (credentialService.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Email or username already in use.");
        }

        Role role = roleService.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
        Team team = request.getTeamId() != null
                ? teamService.findById(request.getTeamId()).orElse(null)
                : null;

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .telegramId(request.getTelegramId())
                .role(role)
                .team(team)
                .status(User.Status.ACTIVE)
                .build();
        user = userService.save(user);

        String salt = java.util.UUID.randomUUID().toString();
        UserCredential credential = UserCredential.builder()
                .user(user)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .passwordSalt(salt)
                .failedAttempts(0)
                .accountLocked(false)
                .deleted(false)
                .build();
        credentialService.save(credential);

        auditLogService.log(actor, "CREATE", "USERS", user.getUserId(), httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> update(@PathVariable Long userId,
                                               @RequestBody UpdateUserRequest request,
                                               @AuthenticationPrincipal User actor,
                                               HttpServletRequest httpRequest) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getTelegramId() != null) user.setTelegramId(request.getTelegramId());
        if (request.getRoleId() != null) {
            Role role = roleService.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
            user.setRole(role);
        }
        if (request.getTeamId() != null) {
            Team team = teamService.findById(request.getTeamId()).orElse(null);
            user.setTeam(team);
        }
        if (request.getStatus() != null) {
            user.setStatus(User.Status.valueOf(request.getStatus()));
        }

        user = userService.save(user);
        auditLogService.log(actor, "UPDATE", "USERS", userId, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> delete(@PathVariable Long userId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        userService.softDelete(userId);
        auditLogService.log(actor, "DELETE", "USERS", userId, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(Map.of(
                "message", "User soft-deleted successfully.",
                "user_id", userId,
                "deleted_at", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{userId}/workload")
    public ResponseEntity<WorkloadResponse> getWorkload(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        List<Task> tasks = taskService.findByAssignee(userId);
        int total = tasks.size();
        int inProgress = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count();
        int pending = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.PENDING).count();
        int done = (int) tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
        int workloadPercent = total > 0 ? (int) ((double) (inProgress + pending) / total * 100) : 0;

        return ResponseEntity.ok(WorkloadResponse.builder()
                .userId(userId)
                .fullName(user.getFullName())
                .assignedTasksTotal(total)
                .inProgressTasks(inProgress)
                .pendingTasks(pending)
                .doneTasks(done)
                .workloadPercent(workloadPercent)
                .atCapacity(workloadPercent >= 90)
                .build());
    }
}
