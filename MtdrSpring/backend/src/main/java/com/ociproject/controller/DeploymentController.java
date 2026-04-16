package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.CreateDeploymentRequest;
import com.ociproject.dto.request.UpdateDeploymentRequest;
import com.ociproject.dto.response.DeploymentResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Deployments", description = "Deployment tracking per project and environment")
@RestController
@RequestMapping("/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final ProjectService projectService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "project_id", required = false) Long projectId,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<Deployment> deployments;
        if (projectId != null && status != null) {
            deployments = deploymentService.findByProjectAndStatus(projectId, Deployment.Status.valueOf(status));
        } else if (projectId != null) {
            deployments = deploymentService.findByProject(projectId);
        } else if (status != null) {
            deployments = deploymentService.findByStatus(Deployment.Status.valueOf(status));
        } else if (environment != null) {
            deployments = deploymentService.findByEnvironment(Deployment.Environment.valueOf(environment));
        } else {
            deployments = deploymentService.findByProject(0L); // empty fallback
        }

        List<DeploymentResponse> data = deployments.stream()
                .map(DeploymentResponse::from)
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<DeploymentResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<DeploymentResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateDeploymentRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Project project = projectService.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        Deployment deployment = Deployment.builder()
                .project(project)
                .version(request.getVersion())
                .environment(request.getEnvironment() != null
                        ? Deployment.Environment.valueOf(request.getEnvironment()) : null)
                .status(request.getStatus() != null
                        ? Deployment.Status.valueOf(request.getStatus())
                        : Deployment.Status.IN_PROGRESS)
                .build();
        deployment = deploymentService.save(deployment);
        auditLogService.log(actor, "CREATE", "DEPLOYMENTS", deployment.getDeploymentId(), httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(DeploymentResponse.from(deployment));
    }

    @PutMapping("/{deploymentId}")
    public ResponseEntity<?> update(@PathVariable Long deploymentId,
                                    @RequestBody UpdateDeploymentRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Deployment deployment = deploymentService.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found."));

        if (request.getStatus() != null) {
            deployment.setStatus(Deployment.Status.valueOf(request.getStatus()));
        }
        if (request.getRecoveryTimeMin() != null) {
            deployment.setRecoveryTimeMin(request.getRecoveryTimeMin());
        }

        deployment = deploymentService.save(deployment);
        auditLogService.log(actor, "UPDATE", "DEPLOYMENTS", deploymentId, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(DeploymentResponse.from(deployment));
    }
}
