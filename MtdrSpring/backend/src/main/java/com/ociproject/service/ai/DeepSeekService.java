package com.ociproject.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ociproject.config.AiProperties;
import com.ociproject.dto.request.ChatTurn;
import com.ociproject.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final AiProperties props;

    public DeepSeekService(RestTemplate aiRestTemplate, AiProperties props) {
        this.restTemplate = aiRestTemplate;
        this.props = props;
    }

    public String chat(String systemPrompt, List<ChatTurn> history, String userMessage, String contextBlock) {
        AiProperties.Deepseek ds = props.getDeepseek();

        List<Map<String, Object>> messages = new ArrayList<>();
        StringBuilder system = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) system.append(systemPrompt);
        if (contextBlock != null && !contextBlock.isBlank()) {
            if (system.length() > 0) system.append("\n\n");
            system.append("## Contexto recuperado\n").append(contextBlock);
        }
        if (system.length() > 0) {
            messages.add(Map.of("role", "system", "content", system.toString()));
        }
        if (history != null) {
            for (ChatTurn t : history) {
                if (t == null || t.getRole() == null || t.getContent() == null) continue;
                messages.add(Map.of("role", t.getRole(), "content", t.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", ds.getModel());
        body.put("messages", messages);
        body.put("temperature", ds.getTemperature());
        body.put("max_tokens", ds.getMaxTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ds.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    ds.getApiUrl(), HttpMethod.POST, entity, String.class);
            JsonNode root = MAPPER.readTree(resp.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new AiServiceException("DeepSeek response has no choices: " + resp.getBody());
            }
            String content = choices.get(0).path("message").path("content").asText(null);
            if (content == null) {
                throw new AiServiceException("DeepSeek response missing content");
            }
            return content;
        } catch (RestClientResponseException e) {
            log.error("DeepSeek HTTP {} error: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException("DeepSeek call failed (" + e.getStatusCode() + ")", e);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek call error", e);
            throw new AiServiceException("DeepSeek call failed: " + e.getMessage(), e);
        }
    }
}
