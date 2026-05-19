package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.recommendation.adapter.EmbeddingModelAdapter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private static final String EN_LANGUAGE_CODE = "en";


    private final EmbeddingModelAdapter embeddingModelAdapter;


    public float[] generateEmbedding(Book book) {
        var embeddingInput = constructEmbeddingInput(book);
        return embeddingModelAdapter.embed(embeddingInput);
    }

    public List<float[]> generateEmbeddings(List<Book> books) {
        var embeddingInputs = books.stream()
                .map(this::constructEmbeddingInput)
                .toList();

        return embeddingModelAdapter.embed(embeddingInputs);
    }

    private String constructEmbeddingInput(Book book) {
        var englishBookTranslation = getEnglishBookTranslation(book);
        var englishCategoryTranslation = getEnglishCategoryTranslation(book.getCategory());

        return constructEmbeddingInput(englishBookTranslation, englishCategoryTranslation);
    }

    private BookTranslation getEnglishBookTranslation(Book book) {
        var translations = book.getTranslations();
        Objects.requireNonNull(translations, "Book translations must not be null");

        return Objects.requireNonNull(translations.get(EN_LANGUAGE_CODE), "English translation must not be null");
    }

    private CategoryTranslation getEnglishCategoryTranslation(Category category) {
        var translations = category.getTranslations();
        Objects.requireNonNull(translations, "Category translations must not be null");

        return Objects.requireNonNull(translations.get(EN_LANGUAGE_CODE), "English translation must not be null");
    }

    private String constructEmbeddingInput(BookTranslation bookTranslation, CategoryTranslation categoryTranslation) {
        String embeddingInput = String.format("Title: %s. Category: %s. Description: %s",
                bookTranslation.getTitle(),
                categoryTranslation.getName(),
                bookTranslation.getDescription());

        return embeddingInput.trim();
    }

}
