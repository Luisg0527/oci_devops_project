package com.ociproject.service;

import com.ociproject.model.BotInteraction;
import com.ociproject.repository.BotInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BotInteractionService {

    private final BotInteractionRepository botInteractionRepository;

    public Page<BotInteraction> findByUser(Long userId, Pageable pageable) {
        return botInteractionRepository.findByUserUserId(userId, pageable);
    }

    @Transactional
    public BotInteraction save(BotInteraction interaction) {
        return botInteractionRepository.save(interaction);
    }
}
