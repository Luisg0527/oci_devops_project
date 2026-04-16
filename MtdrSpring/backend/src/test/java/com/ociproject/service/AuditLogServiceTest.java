package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.AuditLog;
import com.ociproject.model.User;
import com.ociproject.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    AuditLogRepository auditLogRepository;

    @InjectMocks
    AuditLogService service;

    private User actor() {
        return TestFixtures.user5();
    }

    @Test
    void findByUser_returnsPaged() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().auditId(1L).build()));
        when(auditLogRepository.findByUserUserId(eq(5L), any(Pageable.class))).thenReturn(page);

        Page<AuditLog> result = service.findByUser(5L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByEntity_returnsMatchingLogs() {
        AuditLog log = AuditLog.builder().auditId(1L).entityName("TASKS").entityId(99L).build();
        when(auditLogRepository.findByEntityNameAndEntityId("TASKS", 99L)).thenReturn(List.of(log));

        List<AuditLog> result = service.findByEntity("TASKS", 99L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntityName()).isEqualTo("TASKS");
    }

    @Test
    void findByActionType_returnsPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().build()));
        when(auditLogRepository.findByActionType(eq("DELETE"), any(Pageable.class))).thenReturn(page);

        assertThat(service.findByActionType("DELETE", pageable).getTotalElements()).isEqualTo(1);
    }

    @Test
    void log_buildsEntryWithCorrectFieldsAndSaves() {
        User user = actor();
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditLog saved = service.log(user, "CREATE", "TASKS", 42L, "192.168.1.1");

        assertThat(saved.getActionType()).isEqualTo("CREATE");
        assertThat(saved.getEntityName()).isEqualTo("TASKS");
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getUser().getUserId()).isEqualTo(5L);
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}
