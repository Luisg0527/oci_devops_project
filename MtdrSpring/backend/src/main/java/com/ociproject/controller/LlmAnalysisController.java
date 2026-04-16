package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.TriggerAnalysisRequest;
import com.ociproject.dto.response.LlmAnalysisResponse;
import com.ociproject.model.*;
import com.ociproject.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "LLM Analysis", description = "AI-generated insights and anomaly detection")
@RestController
@RequestMapping("/llm-analysis")
@RequiredArgsConstructor
public class LlmAnalysisController {

    private final LlmAnalysisService llmAnalysisService;
    private final ProjectService projectService;
    private final UserService userService;
    private final SprintService sprintService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "scope_type", required = false) String scopeType,
            @RequestParam(name = "user_id", required = false) Long userId,
            @RequestParam(name = "project_id", required = false) Long projectId,
            @RequestParam(name = "sprint_id", required = false) Long sprintId,
            @RequestParam(name = "anomaly_only", defaultValue = "false") boolean anomalyOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LlmAnalysis> analyses;
        if (anomalyOnly) {
            analyses = llmAnalysisService.findAnomalies();
        } else if (scopeType != null) {
            LlmAnalysis.ScopeType scope = LlmAnalysis.ScopeType.valueOf(scopeType);
            switch (scope) {
                case USER -> analyses = llmAnalysisService.findByUser(userId != null ? userId : 0L);
                case PROJECT -> analyses = llmAnalysisService.findByProject(projectId != null ? projectId : 0L);
                case SPRINT -> analyses = llmAnalysisService.findBySprint(sprintId != null ? sprintId : 0L);
                default -> analyses = llmAnalysisService.findAnomalies();
            }
        } else if (projectId != null) {
            analyses = llmAnalysisService.findByProject(projectId);
        } else if (userId != null) {
            analyses = llmAnalysisService.findByUser(userId);
        } else if (sprintId != null) {
            analyses = llmAnalysisService.findBySprint(sprintId);
        } else {
            analyses = llmAnalysisService.findAnomalies();
        }

        List<LlmAnalysisResponse> data = analyses.stream()
                .map(LlmAnalysisResponse::from)
                .collect(Collectors.toList());

        int start = (page - 1) * limit;
        int end = Math.min(start + limit, data.size());
        List<LlmAnalysisResponse> paged = start < data.size() ? data.subList(start, end) : List.of();

        return ResponseEntity.ok(PaginatedResponse.<LlmAnalysisResponse>builder()
                .total(data.size())
                .page(page)
                .limit(limit)
                .data(paged)
                .build());
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerAnalysis(@Valid @RequestBody TriggerAnalysisRequest request) {
        LlmAnalysis.ScopeType scope = LlmAnalysis.ScopeType.valueOf(request.getScopeType());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Analysis triggered. Result will be available shortly.");
        response.put("scope_type", scope.name());

        switch (scope) {
            case USER -> response.put("user_id", request.getUserId());
            case PROJECT -> response.put("project_id", request.getProjectId());
            case SPRINT -> response.put("sprint_id", request.getSprintId());
            default -> {}
        }
        response.put("estimated_completion_seconds", 10);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
