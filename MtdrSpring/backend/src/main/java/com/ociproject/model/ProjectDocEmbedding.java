package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PROJECT_DOC_EMBEDDINGS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ProjectDocEmbedding {

    public enum SourceType {
        PROJECT, SPRINT, TASK, USER, KPI_VALUE, DEPLOYMENT, INCIDENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE_TYPE", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "SOURCE_ID", nullable = false)
    private Long sourceId;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Lob
    @Column(name = "EMBEDDING", nullable = false)
    private byte[] embedding;

    @Column(name = "EMBED_DIM", nullable = false)
    private Integer embedDim;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
