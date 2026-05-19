package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.author.domain.Author;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.recommendation.adapter.EmbeddingModelAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModelAdapter embeddingModelAdapter;

    public float[] generateEmbedding(Book book) {
        var embeddingInput = constructEmbeddingInput(book);
        return embeddingModelAdapter.embed(embeddingInput);
    }

    @Transactional(readOnly = true)
    public List<float[]> generateEmbeddings(List<Book> books) {
        var embeddingInputs = books.stream()
                .map(this::constructEmbeddingInput)
                .toList();

        return embeddingModelAdapter.embed(embeddingInputs);
    }

    private String constructEmbeddingInput(Book book) {
        var englishBookTranslation = getDefaultBookTranslation(book);
        var englishCategoryTranslation = getDefaultCategoryTranslation(book.getCategory());
        var authors = book.getAuthors().stream().
                map(this::getAuthorDefaultTranslationName)
                .collect(Collectors.joining(", "));

        return constructEmbeddingInput(englishBookTranslation, englishCategoryTranslation, authors);
    }

    private String getAuthorDefaultTranslationName(Author author) {
        var defaultTranslation = Objects.requireNonNull(author.getDefaultTranslation(), "Default translation must not be null");
        return defaultTranslation.getFullName();
    }

    private BookTranslation getDefaultBookTranslation(Book book) {
        return Objects.requireNonNull(book.getDefaultTranslation(), "Default translation must not be null");
    }

    private CategoryTranslation getDefaultCategoryTranslation(Category category) {
        return Objects.requireNonNull(category.getDefaultTranslation(), "Default translation must not be null");
    }

    private String constructEmbeddingInput(BookTranslation bookTranslation, CategoryTranslation categoryTranslation, String authors) {
        String embeddingInput = String.format("Title: %s. Category: %s. Authors: %s, Description: %s",
                bookTranslation.getTitle(),
                categoryTranslation.getName(),
                authors,
                bookTranslation.getDescription());

        return embeddingInput.trim();
    }

}
