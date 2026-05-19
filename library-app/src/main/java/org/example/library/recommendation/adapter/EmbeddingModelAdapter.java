package org.example.library.recommendation.adapter;

import java.util.List;

public interface EmbeddingModelAdapter {

    float[] embed(String text);

    List<float[]> embed(List<String> texts);

}
