package com.ociproject.service;

import com.ociproject.model.LlmAnalysis;
import com.ociproject.repository.LlmAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LlmAnalysisService {

    private final LlmAnalysisRepository analysisRepository;

    public Optional<LlmAnalysis> findById(Long id) {
        return analysisRepository.findById(id);
    }

    public List<LlmAnalysis> findByProject(Long projectId) {
        return analysisRepository.findByProjectProjectId(projectId);
    }

    public List<LlmAnalysis> findByUser(Long userId) {
        return analysisRepository.findByUserUserId(userId);
    }

    public List<LlmAnalysis> findBySprint(Long sprintId) {
        return analysisRepository.findBySprintSprintId(sprintId);
    }

    public List<LlmAnalysis> findAnomalies() {
        return analysisRepository.findByAnomalyDetectedTrue();
    }

    @Transactional
    public LlmAnalysis save(LlmAnalysis analysis) {
        validateScope(analysis);
        return analysisRepository.save(analysis);
    }

    private void validateScope(LlmAnalysis analysis) {
        switch (analysis.getScopeType()) {
            case USER    -> { if (analysis.getUser() == null)    throw new IllegalArgumentException("USER scope requires a user"); }
            case PROJECT -> { if (analysis.getProject() == null) throw new IllegalArgumentException("PROJECT scope requires a project"); }
            case SPRINT  -> { if (analysis.getSprint() == null)  throw new IllegalArgumentException("SPRINT scope requires a sprint"); }
            case GLOBAL  -> { /* no FK required */ }
        }
    }
}
