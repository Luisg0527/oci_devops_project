package com.ociproject.repository;

import com.ociproject.model.LlmAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LlmAnalysisRepository extends JpaRepository<LlmAnalysis, Long> {
    List<LlmAnalysis> findByProjectProjectId(Long projectId);
    List<LlmAnalysis> findByUserUserId(Long userId);
    List<LlmAnalysis> findBySprintSprintId(Long sprintId);
    List<LlmAnalysis> findByAnomalyDetectedTrue();
    List<LlmAnalysis> findByScopeType(LlmAnalysis.ScopeType scopeType);
}
