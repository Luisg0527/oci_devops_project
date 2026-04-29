package com.ociproject.controller;

import com.ociproject.config.AiProperties;
import com.ociproject.dto.request.AiAskRequest;
import com.ociproject.dto.response.AiAnswerResponse;
import com.ociproject.model.User;
import com.ociproject.service.ai.AIInsightsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "AI Insights", description = "DeepSeek-backed AI chat over project state (dump mode)")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIInsightsController {

    private final AIInsightsService aiInsightsService;
    private final AiProperties aiProperties;

    @PostMapping("/ask")
    public ResponseEntity<AiAnswerResponse> ask(@Valid @RequestBody AiAskRequest req,
                                                @AuthenticationPrincipal User principal) {
        AiAnswerResponse resp = aiInsightsService.answer(principal, req.getQuestion(), req.getHistory());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "dump");
        body.put("maxContextChars", aiProperties.getRag().getMaxContextChars());
        String key = aiProperties.getDeepseek().getApiKey();
        body.put("deepseekConfigured", key != null && !key.isBlank() && !"sk-test".equals(key));
        body.put("status", "OK");
        return ResponseEntity.ok(body);
    }
}
