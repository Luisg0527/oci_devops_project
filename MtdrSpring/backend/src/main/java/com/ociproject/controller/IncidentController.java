package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.CreateIncidentRequest;
import com.ociproject.dto.request.UpdateIncidentRequest;
import com.ociproject.dto.response.IncidentResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Incidents", description = "Incident CRUD by severity and resolution status")
@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final ProjectService projectService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "project_id", required = false) Long projectId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<Incident> incidents;
        if (projectId != null) {
            incidents = incidentService.findByProject(projectId);
        } else if (severity != null) {
            incidents = incidentService.findBySeverity(Incident.Severity.valueOf(severity));
        } else if (Boolean.FALSE.equals(resolved)) {
            incidents = incidentService.findUnresolved();
        } else {
            incidents = incidentService.findUnresolved();
        }

        if (Boolean.TRUE.equals(resolved)) {
            incidents = incidents.stream().filter(i -> i.getResolvedAt() != null).collect(Collectors.toList());
        } else if (Boolean.FALSE.equals(resolved)) {
            incidents = incidents.stream().filter(i -> i.getResolvedAt() == null).collect(Collectors.toList());
        }

        List<IncidentResponse> data = incidents.stream()
                .map(IncidentResponse::from)
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<IncidentResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<IncidentResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateIncidentRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Project project = projectService.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        Incident incident = Incident.builder()
                .project(project)
                .type(request.getType())
                .description(request.getDescription())
                .severity(request.getSeverity() != null
                        ? Incident.Severity.valueOf(request.getSeverity()) : null)
                .deleted(false)
                .build();
        incident = incidentService.save(incident);
        auditLogService.log(actor, "CREATE", "INCIDENTS", incident.getIncidentId(), httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(incident));
    }

    @PutMapping("/{incidentId}")
    public ResponseEntity<?> update(@PathVariable Long incidentId,
                                    @RequestBody UpdateIncidentRequest request,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        Incident incident = incidentService.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found."));

        if (request.getType() != null) incident.setType(request.getType());
        if (request.getDescription() != null) incident.setDescription(request.getDescription());
        if (request.getSeverity() != null) {
            incident.setSeverity(Incident.Severity.valueOf(request.getSeverity()));
        }
        if (request.getResolvedAt() != null) {
            incident.setResolvedAt(request.getResolvedAt());
        }

        incident = incidentService.save(incident);
        auditLogService.log(actor, "UPDATE", "INCIDENTS", incidentId, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(IncidentResponse.from(incident));
    }

    @DeleteMapping("/{incidentId}")
    public ResponseEntity<?> delete(@PathVariable Long incidentId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        incidentService.softDelete(incidentId);
        auditLogService.log(actor, "DELETE", "INCIDENTS", incidentId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Incident soft-deleted successfully."));
    }
}
