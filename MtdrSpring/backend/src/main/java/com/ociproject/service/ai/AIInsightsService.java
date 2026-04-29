package com.ociproject.service.ai;

import com.ociproject.config.AiProperties;
import com.ociproject.dto.request.ChatTurn;
import com.ociproject.dto.response.AiAnswerResponse;
import com.ociproject.dto.response.AiAnswerResponse.Citation;
import com.ociproject.model.BotInteraction;
import com.ociproject.model.BotInteractionId;
import com.ociproject.model.User;
import com.ociproject.service.BotInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AIInsightsService.class);

    private static final String SYSTEM_PROMPT = """
            Eres un asistente del sistema de gestion de proyectos OCI.
            Responde unicamente con base en el contexto proporcionado.
            Si la pregunta esta en espanol, responde en espanol; si esta en ingles, responde en ingles.
            Cuando uses informacion del contexto, cita la fuente entre corchetes con el formato [TIPO#id], por ejemplo [TASK#42] o [SPRINT#5].
            Si el contexto es insuficiente, dilo explicitamente y no inventes datos.
            Se conciso y directo.
            """;

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final DeepSeekService deepSeekService;
    private final BotInteractionService botInteractionService;
    private final AiProperties props;

    public AIInsightsService(EmbeddingService embeddingService,
                             VectorStore vectorStore,
                             DeepSeekService deepSeekService,
                             BotInteractionService botInteractionService,
                             AiProperties props) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.deepSeekService = deepSeekService;
        this.botInteractionService = botInteractionService;
        this.props = props;
    }

    public AiAnswerResponse answer(User askingUser, String question, List<ChatTurn> history) {
        float[] queryVec = embeddingService.embedQuery(question);
        int k = Math.max(1, props.getRag().getTopK());
        List<ScoredDoc> top = vectorStore.topK(queryVec, k, null);

        String contextBlock = buildContextBlock(top, props.getRag().getMaxContextChars());
        String reply = deepSeekService.chat(SYSTEM_PROMPT, history, question, contextBlock);

        List<Citation> citations = new ArrayList<>(top.size());
        for (ScoredDoc d : top) {
            citations.add(Citation.builder()
                    .sourceType(d.sourceType().name())
                    .sourceId(d.sourceId())
                    .score(round4(d.score()))
                    .build());
        }

        try {
            BotInteractionId id = new BotInteractionId(null, LocalDateTime.now());
            BotInteraction record = BotInteraction.builder()
                    .id(id)
                    .user(askingUser)
                    .message(question)
                    .response(reply)
                    .build();
            botInteractionService.save(record);
        } catch (Exception e) {
            log.warn("Failed to persist BotInteraction: {}", e.getMessage());
        }

        return AiAnswerResponse.builder()
                .answer(reply)
                .citations(citations)
                .build();
    }

    private static String buildContextBlock(List<ScoredDoc> docs, int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (ScoredDoc d : docs) {
            String header = "[" + d.sourceType().name() + "#" + d.sourceId() + "]\n";
            String entry = header + d.content().trim() + "\n---\n";
            if (sb.length() + entry.length() > maxChars) break;
            sb.append(entry);
        }
        return sb.toString();
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
