package com.ociproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IngestionReport {
    @Builder.Default
    private Map<String, Integer> upserted = new LinkedHashMap<>();
    private long elapsedMs;
    @Builder.Default
    private List<String> errors = new java.util.ArrayList<>();
}
