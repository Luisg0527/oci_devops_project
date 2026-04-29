package com.ociproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiAnswerResponse {

    private String answer;
    private List<Citation> citations;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Citation {
        private String sourceType;
        private Long sourceId;
        private double score;
    }
}
