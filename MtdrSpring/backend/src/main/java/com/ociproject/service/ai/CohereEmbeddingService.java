package com.ociproject.service.ai;

import com.ociproject.config.AiProperties;
import com.ociproject.exception.AiServiceException;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.EmbedTextDetails;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.requests.EmbedTextRequest;
import com.oracle.bmc.generativeaiinference.responses.EmbedTextResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Primary
public class CohereEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(CohereEmbeddingService.class);

    private final ObjectProvider<GenerativeAiInferenceClient> clientProvider;
    private final AiProperties props;

    public CohereEmbeddingService(ObjectProvider<GenerativeAiInferenceClient> clientProvider, AiProperties props) {
        this.clientProvider = clientProvider;
        this.props = props;
    }

    @Override
    public float[] embedDocument(String text) {
        return embedBatch(List.of(text), InputType.SEARCH_DOCUMENT).get(0);
    }

    @Override
    public float[] embedQuery(String text) {
        return embedBatch(List.of(text), InputType.SEARCH_QUERY).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts, InputType inputType) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        GenerativeAiInferenceClient client;
        try {
            client = clientProvider.getObject();
        } catch (Exception e) {
            throw new AiServiceException("OCI Generative AI client not available: " + e.getMessage(), e);
        }

        int batchSize = Math.max(1, Math.min(96, props.getRag().getEmbedBatchSize()));
        List<float[]> all = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<String> chunk = texts.subList(start, end);

            EmbedTextDetails details = EmbedTextDetails.builder()
                    .servingMode(OnDemandServingMode.builder()
                            .modelId(props.getOci().getEmbeddingModelId())
                            .build())
                    .compartmentId(props.getOci().getCompartmentId())
                    .inputs(chunk)
                    .truncate(EmbedTextDetails.Truncate.End)
                    .inputType(toOciInputType(inputType))
                    .build();

            EmbedTextRequest request = EmbedTextRequest.builder()
                    .embedTextDetails(details)
                    .build();

            try {
                EmbedTextResponse resp = client.embedText(request);
                List<List<Float>> raw = resp.getEmbedTextResult().getEmbeddings();
                for (List<Float> v : raw) {
                    float[] arr = new float[v.size()];
                    for (int i = 0; i < arr.length; i++) arr[i] = v.get(i);
                    all.add(arr);
                }
            } catch (Exception e) {
                log.error("OCI embedText failed for batch [{}, {})", start, end, e);
                throw new AiServiceException("OCI embedding failed: " + e.getMessage(), e);
            }
        }
        return all;
    }

    private static EmbedTextDetails.InputType toOciInputType(InputType t) {
        return switch (t) {
            case SEARCH_DOCUMENT -> EmbedTextDetails.InputType.SearchDocument;
            case SEARCH_QUERY    -> EmbedTextDetails.InputType.SearchQuery;
        };
    }
}
