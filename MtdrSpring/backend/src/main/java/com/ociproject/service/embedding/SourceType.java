package com.ociproject.service.embedding;

/**
 * Logical document types stored in the vector index.
 * Mirrors the SOURCE_TYPE check constraint originally drafted in
 * {@code 01_create_project_doc_embeddings.sql} so future migrations stay aligned.
 */
public enum SourceType {
    PROJECT,
    SPRINT,
    TASK,
    USER,
    TEAM,
    KPI_VALUE,
    DEPLOYMENT,
    INCIDENT
}
