package com.ociproject.controller;

import com.ociproject.dto.request.AiAskRequest;
import com.ociproject.dto.response.AiAnswerResponse;
import com.ociproject.dto.response.IngestionReport;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.model.User;
import com.ociproject.service.ai.AIInsightsService;
import com.ociproject.service.ai.IngestionService;
import com.ociproject.service.ai.VectorStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "AI Insights", description = "RAG-backed AI chat over project state")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIInsightsController {

    private final AIInsightsService aiInsightsService;
    private final IngestionService ingestionService;
    private final VectorStore vectorStore;

    @PostMapping("/ask")
    public ResponseEntity<AiAnswerResponse> ask(@Valid @RequestBody AiAskRequest req,
                                                @AuthenticationPrincipal User principal) {
        AiAnswerResponse resp = aiInsightsService.answer(principal, req.getQuestion(), req.getHistory());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reindex")
    public ResponseEntity<IngestionReport> reindexAll() {
        IngestionReport report = ingestionService.reindexAll();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(report);
    }

    @PostMapping("/reindex/{type}")
    public ResponseEntity<IngestionReport> reindexOne(@PathVariable("type") String type) {
        SourceType t = SourceType.valueOf(type.toUpperCase());
        IngestionReport report = ingestionService.reindex(t);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(report);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vectorCount", vectorStore.count());
        body.put("status", "OK");
        return ResponseEntity.ok(body);
    }
}
