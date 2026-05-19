package com.ociproject.controller;

import com.ociproject.config.GroqProperties;
import com.ociproject.config.RagProperties;
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
import com.ociproject.service.embedding.EmbeddingClient;
import com.ociproject.service.embedding.EmbeddingService;
import com.ociproject.service.embedding.QdrantVectorStore;
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

@Tag(name = "AI Chat", description = "Sprintly: asistente AI con contexto de la organización (Groq + RAG)")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final GroqClient groqClient;
    private final AiContextService aiContextService;
    private final BotInteractionService botInteractionService;
    private final GroqProperties groqProps;

    // RAG dependencies (all required so Spring fails fast if wiring breaks)
    private final RagProperties ragProps;
    private final EmbeddingClient embeddingClient;
    private final QdrantVectorStore vectorStore;
    private final EmbeddingService embeddingService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody AiChatRequest request,
                                  @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado."));
        }

        long start = System.currentTimeMillis();

        try {
            ContextBundle ctx = buildContext(request.getMessage());
            List<GroqMessage> messages = buildMessages(request, currentUser, ctx);

            String reply = groqClient.complete(messages);

            persistInteraction(currentUser, request.getMessage(), reply);

            long elapsed = System.currentTimeMillis() - start;
            log.info("AI chat OK userId={} mode={} hits={} ({}ms)",
                    currentUser.getUserId(), ctx.mode, ctx.hits.size(), elapsed);

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

    /**
     * Builds the context to feed the LLM. Prefers the RAG path (mini-snapshot
     * + top-K retrieved docs) and silently falls back to the full snapshot if
     * any dependency is missing or fails — the user must never see RAG outages.
     */
    private ContextBundle buildContext(String query) {
        if (ragProps.isEnabled() && embeddingClient.isConfigured()) {
            try {
                float[] qvec = embeddingClient.embed(query);
                List<QdrantVectorStore.Hit> hits =
                        vectorStore.search(qvec, ragProps.getTopK(), null);
                List<EmbeddingService.HydratedDoc> docs = embeddingService.hydrate(hits);
                if (docs.size() > ragProps.getHydratedLimit()) {
                    docs = new ArrayList<>(docs.subList(0, ragProps.getHydratedLimit()));
                }
                return new ContextBundle("rag", null, docs, hits);
            } catch (Exception e) {
                log.warn("RAG retrieval failed ({}). Falling back to full snapshot.", e.getMessage());
            }
        }
        String full = aiContextService.buildSnapshotJson();
        return new ContextBundle("snapshot", full, Collections.emptyList(), Collections.emptyList());
    }

    private List<GroqMessage> buildMessages(AiChatRequest request, User currentUser, ContextBundle ctx) {
        List<GroqMessage> messages = new ArrayList<>();
        messages.add(new GroqMessage("system", buildSystemPrompt(currentUser, ctx)));

        if (request.getHistory() != null) {
            int from = Math.max(0, request.getHistory().size() - 5);
            for (int i = from; i < request.getHistory().size(); i++) {
                AiChatMessageDto m = request.getHistory().get(i);
                String role = "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
                messages.add(new GroqMessage(role, m.getContent()));
            }
        }

        messages.add(new GroqMessage("user", request.getMessage()));
        return messages;
    }

    private String buildSystemPrompt(User currentUser, ContextBundle ctx) {
        String userName = currentUser.getFullName() != null ? currentUser.getFullName() : "Usuario";
        String role = roleFromContext();

        StringBuilder sb = new StringBuilder();
        sb.append("Eres \"Sprintly\", asistente virtual del equipo de gestión de proyectos.\n")
          .append("Hablas español de forma profesional pero cercana y directa. Tu trabajo:\n")
          .append("- Resumir el estado de sprints, tasks, equipos y proyectos cuando te lo pidan.\n")
          .append("- Detectar riesgos: tasks atrasadas, cargas desbalanceadas, sprints en peligro.\n")
          .append("- Dar opiniones y recomendaciones accionables para agilizar la toma de decisiones.\n")
          .append("- Citar datos concretos por NOMBRE (sprints, tareas, personas), fechas y porcentajes.\n")
          .append("- NUNCA muestres IDs numéricos al usuario; los IDs en el JSON son solo para tu uso interno.\n")
          .append("- Si la pregunta no se puede responder con los datos provistos, dilo honestamente.\n")
          .append("- Sé MUY conciso: ve directo al punto, usa bullets cortos, evita relleno e introducciones.\n")
          .append("  Limita tus respuestas a lo estrictamente necesario; menos es más.\n\n")
          .append("Usuario actual: ").append(userName).append(" (rol: ").append(role).append(")\n")
          .append("Fecha de hoy: ").append(LocalDate.now()).append("\n\n");

        if ("rag".equals(ctx.mode)) {
            if (!ctx.docs.isEmpty()) {
                sb.append("Documentos relevantes recuperados para esta consulta (top-")
                  .append(ctx.docs.size()).append(" por similitud, más relevante primero):\n");
                int i = 1;
                for (EmbeddingService.HydratedDoc d : ctx.docs) {
                    sb.append(i++).append(". [").append(d.type()).append("] ").append(d.text()).append('\n');
                }
            } else {
                sb.append("(No se encontraron documentos relevantes para esta consulta.)\n");
            }
        } else {
            sb.append("Datos de la organización (JSON):\n").append(ctx.snapshot);
        }
        return sb.toString();
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
            BotInteractionId id = new BotInteractionId(null, LocalDateTime.now());
            BotInteraction interaction = BotInteraction.builder()
                    .id(id)
                    .user(user)
                    .message(truncate(message, 4000))
                    .response(truncate(response, 8000))
                    .build();
            botInteractionService.save(interaction);
        } catch (Exception e) {
            log.warn("No se pudo persistir la interacción en BOT_INTERACTIONS: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Internal context bundle: either RAG ({@code mode="rag"}) or fallback ({@code "snapshot"}). */
    private record ContextBundle(
            String mode,
            String snapshot,
            List<EmbeddingService.HydratedDoc> docs,
            List<QdrantVectorStore.Hit> hits
    ) {}
}
