package org.example.library.recommendation.adapter;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LangChain4jEmbeddingAdapter implements EmbeddingModelAdapter {

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text)
                .content()
                .vector();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<TextSegment> textSegments = toTextSegments(texts);
        return embeddingModel.embedAll(textSegments).content().stream()
                .map(Embedding::vector)
                .toList();
    }

    private List<TextSegment> toTextSegments(List<String> texts) {
        return texts.stream()
                .map(TextSegment::from)
                .toList();
    }

}
