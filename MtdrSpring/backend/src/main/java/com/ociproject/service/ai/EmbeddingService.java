package com.ociproject.service.ai;

import java.util.List;

public interface EmbeddingService {

    enum InputType { SEARCH_DOCUMENT, SEARCH_QUERY }

    float[] embedDocument(String text);

    float[] embedQuery(String text);

    List<float[]> embedBatch(List<String> texts, InputType inputType);
}
