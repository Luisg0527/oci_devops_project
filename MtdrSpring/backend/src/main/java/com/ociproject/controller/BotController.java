package com.ociproject.controller;

import com.ociproject.dto.PaginatedResponse;
import com.ociproject.dto.request.BotMessageRequest;
import com.ociproject.dto.response.BotInteractionResponse;
import com.ociproject.exception.ResourceNotFoundException;
import com.ociproject.model.BotInteraction;
import com.ociproject.model.BotInteractionId;
import com.ociproject.model.User;
import com.ociproject.service.BotInteractionService;
import com.ociproject.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Bot", description = "Telegram bot interaction history and message handling")
@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    private final BotInteractionService botInteractionService;
    private final UserService userService;

    @GetMapping("/interactions")
    public ResponseEntity<?> getInteractions(
            @RequestParam(name = "user_id", required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        if (userId == null) {
            return ResponseEntity.ok(PaginatedResponse.<BotInteractionResponse>builder()
                    .total(0).page(page).limit(limit).data(List.of()).build());
        }

        Page<BotInteraction> interactions = botInteractionService.findByUser(userId,
                PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "id.createdAt")));

        List<BotInteractionResponse> data = interactions.getContent().stream()
                .map(BotInteractionResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(PaginatedResponse.<BotInteractionResponse>builder()
                .total(interactions.getTotalElements())
                .page(page)
                .limit(limit)
                .data(data)
                .build());
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody BotMessageRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Generate a simple bot response based on the message
        String botResponse = generateBotResponse(request.getMessage(), user);

        BotInteractionId id = new BotInteractionId(null, LocalDateTime.now());
        BotInteraction interaction = BotInteraction.builder()
                .id(id)
                .user(user)
                .message(request.getMessage())
                .response(botResponse)
                .build();
        interaction = botInteractionService.save(interaction);

        return ResponseEntity.ok(BotInteractionResponse.from(interaction));
    }

    private String generateBotResponse(String message, User user) {
        if (message == null) return "No entendí tu mensaje.";
        String lower = message.toLowerCase();
        if (lower.contains("/sprint") || lower.contains("sprint actual")) {
            return "Consulta el estado del sprint activo desde el dashboard.";
        }
        if (lower.contains("/tareas") || lower.contains("pendientes")) {
            return "Usa GET /tasks/my para ver tus tareas pendientes, " + user.getFullName() + ".";
        }
        return "Mensaje recibido: " + message + ". Usa /help para ver los comandos disponibles.";
    }
}
