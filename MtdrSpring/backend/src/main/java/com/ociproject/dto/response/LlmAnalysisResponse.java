package com.ociproject.dto.response;

import com.ociproject.model.LlmAnalysis;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LlmAnalysisResponse {
    private Long analysisId;
    private String scopeType;
    private Long projectId;
    private String projectName;
    private Long userId;
    private Long sprintId;
    private Boolean anomalyDetected;
    private String anomalyType;
    private BigDecimal confidenceScore;
    private String recommendation;
    private LocalDateTime analysisDate;

    public static LlmAnalysisResponse from(LlmAnalysis a) {
        return LlmAnalysisResponse.builder()
                .analysisId(a.getAnalysisId())
                .scopeType(a.getScopeType() != null ? a.getScopeType().name() : null)
                .projectId(a.getProject() != null ? a.getProject().getProjectId() : null)
                .projectName(a.getProject() != null ? a.getProject().getName() : null)
                .userId(a.getUser() != null ? a.getUser().getUserId() : null)
                .sprintId(a.getSprint() != null ? a.getSprint().getSprintId() : null)
                .anomalyDetected(a.getAnomalyDetected())
                .anomalyType(a.getAnomalyType())
                .confidenceScore(a.getConfidenceScore())
                .recommendation(a.getRecommendation())
                .analysisDate(a.getAnalysisDate())
                .build();
    }
}
