package com.ociproject.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class BotInteractionId implements Serializable {

    @Column(name = "INTERACTION_ID")
    private Long interactionId;

    // Part of the PK to support Oracle range partitioning by month
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
