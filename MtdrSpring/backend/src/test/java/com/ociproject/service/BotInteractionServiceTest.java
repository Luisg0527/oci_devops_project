package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.BotInteraction;
import com.ociproject.repository.BotInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotInteractionServiceTest {

    @Mock
    BotInteractionRepository botInteractionRepository;

    @InjectMocks
    BotInteractionService service;

    private BotInteraction interaction() {
        // Fixed timestamp — avoids LocalDateTime.now() non-determinism in the composite PK.
        return TestFixtures.botInteraction1();
    }

    @Test
    void findByUser_returnsPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BotInteraction> page = new PageImpl<>(List.of(interaction()));
        when(botInteractionRepository.findByUserUserId(eq(2L), any(Pageable.class))).thenReturn(page);

        Page<BotInteraction> result = service.findByUser(2L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(botInteractionRepository).findByUserUserId(2L, pageable);
    }

    @Test
    void save_persistsInteraction() {
        BotInteraction i = interaction();
        when(botInteractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(i)).isSameAs(i);
        verify(botInteractionRepository).save(i);
    }
}
