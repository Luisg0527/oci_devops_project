package com.ociproject.service.ai;

import com.ociproject.config.AiProperties;
import com.ociproject.dto.request.ChatTurn;
import com.ociproject.dto.response.AiAnswerResponse;
import com.ociproject.dto.response.AiAnswerResponse.Citation;
import com.ociproject.model.BotInteraction;
import com.ociproject.model.BotInteractionId;
import com.ociproject.model.User;
import com.ociproject.service.BotInteractionService;
import com.ociproject.service.ai.ContextDumperService.DocumentRef;
import com.ociproject.service.ai.ContextDumperService.DumpResult;
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

    private final ContextDumperService contextDumperService;
    private final DeepSeekService deepSeekService;
    private final BotInteractionService botInteractionService;
    private final AiProperties props;

    public AIInsightsService(ContextDumperService contextDumperService,
                             DeepSeekService deepSeekService,
                             BotInteractionService botInteractionService,
                             AiProperties props) {
        this.contextDumperService = contextDumperService;
        this.deepSeekService = deepSeekService;
        this.botInteractionService = botInteractionService;
        this.props = props;
    }

    public AiAnswerResponse answer(User askingUser, String question, List<ChatTurn> history) {
        DumpResult dump = contextDumperService.dumpAll(props.getRag().getMaxContextChars());
        String reply = deepSeekService.chat(SYSTEM_PROMPT, history, question, dump.contextBlock());

        List<Citation> citations = new ArrayList<>(dump.included().size());
        for (DocumentRef ref : dump.included()) {
            citations.add(Citation.builder()
                    .sourceType(ref.sourceType().name())
                    .sourceId(ref.sourceId())
                    .score(1.0)
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
}
