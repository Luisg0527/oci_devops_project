package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.response.AuditLogResponse;
import com.ociproject.model.AuditLog;
import com.ociproject.service.AuditLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Audit Log", description = "Append-only audit trail filtered by user, entity, or action")
@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "user_id", required = false) Long userId,
            @RequestParam(name = "entity_name", required = false) String entityName,
            @RequestParam(name = "action_type", required = false) String actionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        if (userId != null) {
            Page<AuditLog> logs = auditLogService.findByUser(userId,
                    PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "actionDate")));
            List<AuditLogResponse> data = logs.getContent().stream()
                    .map(AuditLogResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.<AuditLogResponse>builder()
                    .total(logs.getTotalElements())
                    .page(page)
                    .limit(limit)
                    .data(data)
                    .build());
        }

        if (actionType != null) {
            Page<AuditLog> logs = auditLogService.findByActionType(actionType,
                    PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "actionDate")));
            List<AuditLogResponse> data = logs.getContent().stream()
                    .map(AuditLogResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.<AuditLogResponse>builder()
                    .total(logs.getTotalElements())
                    .page(page)
                    .limit(limit)
                    .data(data)
                    .build());
        }

        if (entityName != null) {
            List<AuditLogResponse> data = auditLogService.findByEntity(entityName, null).stream()
                    .map(AuditLogResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.<AuditLogResponse>builder()
                    .total(data.size())
                    .page(page)
                    .limit(limit)
                    .data(data)
                    .build());
        }

        // Default: return empty
        return ResponseEntity.ok(PaginatedResponse.<AuditLogResponse>builder()
                .total(0)
                .page(page)
                .limit(limit)
                .data(List.of())
                .build());
    }
}
