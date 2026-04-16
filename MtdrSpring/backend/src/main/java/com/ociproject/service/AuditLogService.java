package com.ociproject.service;

import com.ociproject.model.AuditLog;
import com.ociproject.model.User;
import com.ociproject.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> findByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserUserId(userId, pageable);
    }

    public List<AuditLog> findByEntity(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId);
    }

    public Page<AuditLog> findByActionType(String actionType, Pageable pageable) {
        return auditLogRepository.findByActionType(actionType, pageable);
    }

    @Transactional
    public AuditLog log(User user, String actionType, String entityName, Long entityId, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .user(user)
                .actionType(actionType)
                .entityName(entityName)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .build();
        return auditLogRepository.save(entry);
    }
}
