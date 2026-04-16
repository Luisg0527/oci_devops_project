package com.ociproject.dto.response;

import com.ociproject.model.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long auditId;
    private Long userId;
    private String userName;
    private String actionType;
    private String entityName;
    private Long entityId;
    private LocalDateTime actionDate;
    private String ipAddress;

    public static AuditLogResponse from(AuditLog al) {
        return AuditLogResponse.builder()
                .auditId(al.getAuditId())
                .userId(al.getUser() != null ? al.getUser().getUserId() : null)
                .userName(al.getUser() != null ? al.getUser().getFullName() : null)
                .actionType(al.getActionType())
                .entityName(al.getEntityName())
                .entityId(al.getEntityId())
                .actionDate(al.getActionDate())
                .ipAddress(al.getIpAddress())
                .build();
    }
}
