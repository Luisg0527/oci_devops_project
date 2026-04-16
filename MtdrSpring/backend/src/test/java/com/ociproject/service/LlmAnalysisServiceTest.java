package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.*;
import com.ociproject.repository.LlmAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmAnalysisServiceTest {

    @Mock
    LlmAnalysisRepository analysisRepository;

    @InjectMocks
    LlmAnalysisService service;

    private LlmAnalysis analysis(LlmAnalysis.ScopeType scope) {
        return LlmAnalysis.builder()
                .analysisId(1L)
                .scopeType(scope)
                .anomalyDetected(false)
                .build();
    }

    @Test
    void findById_found() {
        LlmAnalysis a = analysis(LlmAnalysis.ScopeType.GLOBAL);
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThat(service.findById(1L)).isPresent().contains(a);
    }

    @Test
    void findByProject_returnsList() {
        when(analysisRepository.findByProjectProjectId(2L)).thenReturn(List.of(analysis(LlmAnalysis.ScopeType.PROJECT)));

        assertThat(service.findByProject(2L)).hasSize(1);
    }

    @Test
    void findByUser_returnsList() {
        when(analysisRepository.findByUserUserId(2L)).thenReturn(List.of(analysis(LlmAnalysis.ScopeType.USER)));

        assertThat(service.findByUser(2L)).hasSize(1);
    }

    @Test
    void findBySprint_returnsList() {
        when(analysisRepository.findBySprintSprintId(2L)).thenReturn(List.of(analysis(LlmAnalysis.ScopeType.SPRINT)));

        assertThat(service.findBySprint(2L)).hasSize(1);
    }

    @Test
    void findAnomalies_returnsAnomalyList() {
        LlmAnalysis anomaly = analysis(LlmAnalysis.ScopeType.PROJECT);
        anomaly.setAnomalyDetected(true);
        when(analysisRepository.findByAnomalyDetectedTrue()).thenReturn(List.of(anomaly));

        assertThat(service.findAnomalies()).hasSize(1);
    }

    // ── Scope validation ───────────────────────────────────────────────────────

    @Test
    void save_globalScope_noFkRequired_saves() {
        LlmAnalysis a = analysis(LlmAnalysis.ScopeType.GLOBAL);
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(a);

        verify(analysisRepository).save(a);
    }

    @Test
    void save_userScope_withUser_saves() {
        LlmAnalysis a = analysis(LlmAnalysis.ScopeType.USER);
        a.setUser(TestFixtures.user2());
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(a);

        verify(analysisRepository).save(a);
    }

    @Test
    void save_userScope_missingUser_throwsIllegalArgument() {
        LlmAnalysis a = analysis(LlmAnalysis.ScopeType.USER); // user null

        assertThrows(IllegalArgumentException.class, () -> service.save(a));
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void save_projectScope_missingProject_throwsIllegalArgument() {
        LlmAnalysis a = analysis(LlmAnalysis.ScopeType.PROJECT); // project null

        assertThrows(IllegalArgumentException.class, () -> service.save(a));
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void save_sprintScope_missingSprint_throwsIllegalArgument() {
        LlmAnalysis a = analysis(LlmAnalysis.ScopeType.SPRINT); // sprint null

        assertThrows(IllegalArgumentException.class, () -> service.save(a));
        verify(analysisRepository, never()).save(any());
    }
}
