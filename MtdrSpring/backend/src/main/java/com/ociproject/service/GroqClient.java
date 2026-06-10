package com.ociproject.service;

import com.ociproject.config.GroqProperties;
import com.ociproject.dto.groq.GroqChatRequest;
import com.ociproject.dto.groq.GroqChatResponse;
import com.ociproject.dto.groq.GroqMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Thin client for the Groq Chat Completions API (OpenAI-compatible).
 * Uses Spring 6 RestClient — no extra dependencies required.
 */
@Service
@RequiredArgsConstructor
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final GroqProperties props;
    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(props.getTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(props.getApiUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Sends a chat completion request to Groq and returns the assistant message text.
     * @throws GroqException on any non-2xx response or transport error.
     */
    public String complete(List<GroqMessage> messages) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()
                || "tu-key-aqui".equals(props.getApiKey())) {
            throw new GroqException(
                    "Groq API key no configurada. Define GROQ_API_KEY en el entorno o en application.properties.");
        }

        GroqChatRequest body = GroqChatRequest.builder()
                .model(props.getModel())
                .messages(messages)
                .temperature(0.4)
                .maxTokens(1200)
                .stream(false)
                .build();

        try {
            GroqChatResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GroqChatResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new GroqException("Groq devolvió una respuesta vacía.");
            }
            GroqMessage reply = response.getChoices().get(0).getMessage();
            if (reply == null || reply.getContent() == null) {
                throw new GroqException("Groq devolvió un mensaje sin contenido.");
            }
            if (response.getUsage() != null) {
                log.debug("Groq tokens: prompt={}, completion={}, total={}",
                        response.getUsage().getPromptTokens(),
                        response.getUsage().getCompletionTokens(),
                        response.getUsage().getTotalTokens());
            }
            return reply.getContent().trim();
        } catch (RestClientResponseException e) {
            log.error("Groq API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GroqException("Groq API respondió " + e.getStatusCode() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("Error llamando a Groq: {}", e.getMessage(), e);
            throw new GroqException("No se pudo contactar a Groq: " + e.getMessage());
        }
    }

    public static class GroqException extends RuntimeException {
        public GroqException(String message) { super(message); }
    }
}
