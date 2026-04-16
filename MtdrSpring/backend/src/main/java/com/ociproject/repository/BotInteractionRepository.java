package com.ociproject.repository;

import com.ociproject.model.BotInteraction;
import com.ociproject.model.BotInteractionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotInteractionRepository extends JpaRepository<BotInteraction, BotInteractionId> {
    Page<BotInteraction> findByUserUserId(Long userId, Pageable pageable);
}
