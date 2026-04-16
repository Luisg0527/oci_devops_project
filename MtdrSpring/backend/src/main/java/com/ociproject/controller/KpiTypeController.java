package com.ociproject.controller;

import com.ociproject.dto.DataResponse;
import com.ociproject.dto.request.CreateKpiTypeRequest;
import com.ociproject.dto.request.UpdateKpiTypeRequest;
import com.ociproject.dto.response.KpiTypeResponse;
import com.ociproject.exception.ResourceNotFoundException;
import com.ociproject.model.KpiType;
import com.ociproject.model.User;
import com.ociproject.service.AuditLogService;
import com.ociproject.service.KpiService;
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

@Tag(name = "KPI Types", description = "KPI type definitions and categories")
@RestController
@RequestMapping("/kpi-types")
@RequiredArgsConstructor
public class KpiTypeController {

    private final KpiService kpiService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<DataResponse<KpiTypeResponse>> getAll(
            @RequestParam(required = false) String category) {
        List<KpiType> types = category != null
                ? kpiService.findTypesByCategory(category)
                : kpiService.findAllTypes();
        List<KpiTypeResponse> data = types.stream()
                .map(KpiTypeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(DataResponse.<KpiTypeResponse>builder().data(data).build());
    }

    @PostMapping
    public ResponseEntity<KpiTypeResponse> create(@Valid @RequestBody CreateKpiTypeRequest request,
                                                  @AuthenticationPrincipal User actor,
                                                  HttpServletRequest httpRequest) {
        KpiType kpiType = KpiType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .unit(request.getUnit())
                .build();
        kpiType = kpiService.saveType(kpiType);
        auditLogService.log(actor, "CREATE", "KPI_TYPES", kpiType.getKpiTypeId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(KpiTypeResponse.from(kpiType));
    }

    @PutMapping("/{kpiTypeId}")
    public ResponseEntity<KpiTypeResponse> update(@PathVariable Long kpiTypeId,
                                                  @RequestBody UpdateKpiTypeRequest request,
                                                  @AuthenticationPrincipal User actor,
                                                  HttpServletRequest httpRequest) {
        KpiType kpiType = kpiService.findTypeById(kpiTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("KPI type not found."));
        if (request.getName() != null) kpiType.setName(request.getName());
        if (request.getDescription() != null) kpiType.setDescription(request.getDescription());
        if (request.getCategory() != null) kpiType.setCategory(request.getCategory());
        if (request.getUnit() != null) kpiType.setUnit(request.getUnit());
        kpiType = kpiService.saveType(kpiType);
        auditLogService.log(actor, "UPDATE", "KPI_TYPES", kpiTypeId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(KpiTypeResponse.from(kpiType));
    }

    @DeleteMapping("/{kpiTypeId}")
    public ResponseEntity<?> delete(@PathVariable Long kpiTypeId,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest httpRequest) {
        kpiService.findTypeById(kpiTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("KPI type not found."));
        auditLogService.log(actor, "DELETE", "KPI_TYPES", kpiTypeId, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "KPI type deleted successfully."));
    }
}
