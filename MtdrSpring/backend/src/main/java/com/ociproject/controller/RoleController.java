package com.ociproject.controller;

import com.ociproject.dto.DataResponse;
import com.ociproject.dto.request.CreateRoleRequest;
import com.ociproject.dto.request.UpdateRoleRequest;
import com.ociproject.dto.response.RoleResponse;
import com.ociproject.exception.ResourceNotFoundException;
import com.ociproject.model.Role;
import com.ociproject.model.User;
import com.ociproject.service.AuditLogService;
import com.ociproject.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Roles", description = "Role management")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<DataResponse<RoleResponse>> getAll() {
        List<RoleResponse> data = roleService.findAll().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(DataResponse.<RoleResponse>builder().data(data).build());
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request,
                                               @AuthenticationPrincipal User actor,
                                               HttpServletRequest httpRequest) {
        Role role = Role.builder()
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .deleted(false)
                .build();
        role = roleService.save(role);
        auditLogService.log(actor, "CREATE", "ROLES", role.getRoleId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> update(@PathVariable Long roleId,
                                               @RequestBody UpdateRoleRequest request,
                                               @AuthenticationPrincipal User actor,
                                               HttpServletRequest httpRequest) {
        Role role = roleService.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
        if (request.getRoleName() != null) role.setRoleName(request.getRoleName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        role = roleService.save(role);
        auditLogService.log(actor, "UPDATE", "ROLES", roleId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(RoleResponse.from(role));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<?> delete(@PathVariable Long roleId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        roleService.softDelete(roleId);
        auditLogService.log(actor, "DELETE", "ROLES", roleId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Role soft-deleted successfully."));
    }
}
