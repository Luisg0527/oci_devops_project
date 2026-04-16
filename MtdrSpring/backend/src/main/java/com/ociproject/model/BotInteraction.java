package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BOT_INTERACTIONS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class BotInteraction {

    @EmbeddedId
    private BotInteractionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Lob
    @Column(name = "MESSAGE")
    private String message;

    @Lob
    @Column(name = "RESPONSE")
    private String response;
}
