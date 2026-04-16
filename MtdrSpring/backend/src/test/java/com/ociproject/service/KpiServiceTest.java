package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.*;
import com.ociproject.repository.KpiTypeRepository;
import com.ociproject.repository.KpiValueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock KpiTypeRepository kpiTypeRepository;
    @Mock KpiValueRepository kpiValueRepository;

    @InjectMocks
    KpiService service;

    private KpiValue kpiValue(KpiValue.ScopeType scope) {
        return KpiValue.builder()
                .kpiType(TestFixtures.kpiType1())
                .scopeType(scope)
                .value(BigDecimal.valueOf(42))
                .build();
    }

    @Test
    void findAllTypes_returnsList() {
        when(kpiTypeRepository.findAll()).thenReturn(List.of(TestFixtures.kpiType1()));

        assertThat(service.findAllTypes()).hasSize(1);
    }

    @Test
    void findTypeById_found() {
        when(kpiTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.kpiType1()));

        assertThat(service.findTypeById(1L)).isPresent();
    }

    @Test
    void findTypesByCategory_returnsList() {
        when(kpiTypeRepository.findByCategory("DELIVERY")).thenReturn(List.of(TestFixtures.kpiType1()));

        assertThat(service.findTypesByCategory("DELIVERY")).hasSize(1);
    }

    @Test
    void saveType_persists() {
        KpiType kt = TestFixtures.kpiType1();
        when(kpiTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.saveType(kt)).isSameAs(kt);
    }

    @Test
    void findValuesByUser_returnsList() {
        when(kpiValueRepository.findByUserUserId(2L)).thenReturn(List.of(kpiValue(KpiValue.ScopeType.USER)));

        assertThat(service.findValuesByUser(2L)).hasSize(1);
    }

    @Test
    void findValuesByProject_returnsList() {
        when(kpiValueRepository.findByProjectProjectId(2L)).thenReturn(List.of(kpiValue(KpiValue.ScopeType.PROJECT)));

        assertThat(service.findValuesByProject(2L)).hasSize(1);
    }

    @Test
    void findValuesBySprint_returnsList() {
        when(kpiValueRepository.findBySprintSprintId(2L)).thenReturn(List.of(kpiValue(KpiValue.ScopeType.SPRINT)));

        assertThat(service.findValuesBySprint(2L)).hasSize(1);
    }

    // ── Scope validation ───────────────────────────────────────────────────────

    @Test
    void recordValue_userScope_withUser_saves() {
        KpiValue kv = kpiValue(KpiValue.ScopeType.USER);
        kv.setUser(TestFixtures.user2());
        when(kpiValueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordValue(kv);

        verify(kpiValueRepository).save(kv);
    }

    @Test
    void recordValue_userScope_missingUser_throwsIllegalArgument() {
        KpiValue kv = kpiValue(KpiValue.ScopeType.USER); // user is null

        assertThrows(IllegalArgumentException.class, () -> service.recordValue(kv));
        verify(kpiValueRepository, never()).save(any());
    }

    @Test
    void recordValue_projectScope_missingProject_throwsIllegalArgument() {
        KpiValue kv = kpiValue(KpiValue.ScopeType.PROJECT); // project is null

        assertThrows(IllegalArgumentException.class, () -> service.recordValue(kv));
        verify(kpiValueRepository, never()).save(any());
    }

    @Test
    void recordValue_sprintScope_missingSprint_throwsIllegalArgument() {
        KpiValue kv = kpiValue(KpiValue.ScopeType.SPRINT); // sprint is null

        assertThrows(IllegalArgumentException.class, () -> service.recordValue(kv));
        verify(kpiValueRepository, never()).save(any());
    }

    @Test
    void recordValue_globalScope_noFkRequired_saves() {
        KpiValue kv = kpiValue(KpiValue.ScopeType.GLOBAL); // all FKs null — valid for GLOBAL
        when(kpiValueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordValue(kv);

        verify(kpiValueRepository).save(kv);
    }
}
