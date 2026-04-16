package com.ociproject.repository;

import com.ociproject.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUserUserId(Long userId, Pageable pageable);
    List<AuditLog> findByEntityNameAndEntityId(String entityName, Long entityId);
    Page<AuditLog> findByActionType(String actionType, Pageable pageable);
}
