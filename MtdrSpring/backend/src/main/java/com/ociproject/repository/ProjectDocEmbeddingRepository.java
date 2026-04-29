package com.ociproject.repository;

import com.ociproject.model.ProjectDocEmbedding;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProjectDocEmbeddingRepository extends JpaRepository<ProjectDocEmbedding, Long> {

    Optional<ProjectDocEmbedding> findBySourceTypeAndSourceId(SourceType sourceType, Long sourceId);

    long countBySourceType(SourceType sourceType);

    void deleteBySourceType(SourceType sourceType);

    @Query("select max(e.updatedAt) from ProjectDocEmbedding e")
    Optional<LocalDateTime> findMaxUpdatedAt();
}
