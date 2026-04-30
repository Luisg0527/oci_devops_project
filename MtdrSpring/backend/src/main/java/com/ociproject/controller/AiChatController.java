package com.ociproject.controller;

import com.ociproject.config.GroqProperties;
import com.ociproject.dto.groq.GroqMessage;
import com.ociproject.dto.request.AiChatMessageDto;
import com.ociproject.dto.request.AiChatRequest;
import com.ociproject.dto.response.AiChatResponse;
import com.ociproject.model.BotInteraction;
import com.ociproject.model.BotInteractionId;
import com.ociproject.model.User;
import com.ociproject.service.AiContextService;
import com.ociproject.service.BotInteractionService;
import com.ociproject.service.GroqClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "AI Chat", description = "Sprintly: asistente AI con contexto de la organización (Groq)")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final GroqClient groqClient;
    private final AiContextService aiContextService;
    private final BotInteractionService botInteractionService;
    private final GroqProperties groqProps;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody AiChatRequest request,
                                  @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado."));
        }

        long start = System.currentTimeMillis();

        try {
            String snapshot = aiContextService.buildSnapshotJson();
            List<GroqMessage> messages = buildMessages(request, currentUser, snapshot);

            String reply = groqClient.complete(messages);

            persistInteraction(currentUser, request.getMessage(), reply);

            long elapsed = System.currentTimeMillis() - start;
            log.info("AI chat OK userId={} ({}ms)", currentUser.getUserId(), elapsed);

            return ResponseEntity.ok(AiChatResponse.builder()
                    .reply(reply)
                    .model(groqProps.getModel())
                    .elapsedMs(elapsed)
                    .build());

        } catch (GroqClient.GroqException e) {
            log.warn("Groq error para userId={}: {}", currentUser.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado en AI chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno procesando la solicitud."));
        }
    }

    private List<GroqMessage> buildMessages(AiChatRequest request, User currentUser, String snapshot) {
        List<GroqMessage> messages = new ArrayList<>();
        messages.add(new GroqMessage("system", buildSystemPrompt(currentUser, snapshot)));

        // History (capped to last 10 turns to bound tokens)
        if (request.getHistory() != null) {
            int from = Math.max(0, request.getHistory().size() - 10);
            for (int i = from; i < request.getHistory().size(); i++) {
                AiChatMessageDto m = request.getHistory().get(i);
                String role = "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
                messages.add(new GroqMessage(role, m.getContent()));
            }
        }

        messages.add(new GroqMessage("user", request.getMessage()));
        return messages;
    }

    private String buildSystemPrompt(User currentUser, String snapshot) {
        // Concatenación directa: el snapshot puede contener '%' literales (descripciones,
        // valores con porcentaje, etc.) que romperían String.format / .formatted().
        String userName = currentUser.getFullName() != null ? currentUser.getFullName() : "Usuario";
        String role = roleFromContext();

        return "Eres \"Sprintly\", asistente virtual del equipo de gestión de proyectos.\n"
                + "Hablas español de forma profesional pero cercana y directa. Tu trabajo:\n"
                + "- Resumir el estado de sprints, tasks, equipos y proyectos cuando te lo pidan.\n"
                + "- Detectar riesgos: tasks atrasadas, cargas desbalanceadas, sprints en peligro.\n"
                + "- Dar opiniones y recomendaciones accionables para agilizar la toma de decisiones.\n"
                + "- Citar datos concretos por NOMBRE (sprints, tareas, personas), fechas y porcentajes.\n"
                + "- NUNCA muestres IDs numéricos al usuario; los IDs en el JSON son solo para tu uso interno.\n"
                + "- Si la pregunta no se puede responder con los datos provistos, dilo honestamente.\n"
                + "- Sé MUY conciso: ve directo al punto, usa bullets cortos, evita relleno e introducciones.\n"
                + "  Limita tus respuestas a lo estrictamente necesario; menos es más.\n\n"
                + "Usuario actual: " + userName + " (rol: " + role + ")\n"
                + "Fecha de hoy: " + LocalDate.now() + "\n\n"
                + "Datos de la organización (JSON):\n"
                + snapshot;
    }

    private String roleFromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return "USER";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(Objects::nonNull)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("USER");
    }

    private void persistInteraction(User user, String message, String response) {
        try {
            // interactionId queda null y la BD/sequence lo asigna; createdAt es parte del PK
            BotInteractionId id = new BotInteractionId(null, LocalDateTime.now());
            BotInteraction interaction = BotInteraction.builder()
                    .id(id)
                    .user(user)
                    .message(truncate(message, 4000))
                    .response(truncate(response, 8000))
                    .build();
            botInteractionService.save(interaction);
        } catch (Exception e) {
            // No bloquear la respuesta del usuario por un fallo al guardar el log.
            log.warn("No se pudo persistir la interacción en BOT_INTERACTIONS: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
