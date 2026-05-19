package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.recommendation.adapter.EmbeddingModelAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModelAdapter embeddingModelAdapter;

    @InjectMocks
    private EmbeddingService embeddingService;


    @Test
    void shouldGenerateEmbeddingSuccessfully() {
        var book = createBook("Fiction", "Test Title", "Test Description");
        float[] expectedVector = new float[]{0.1f, 0.2f};

        when(embeddingModelAdapter.embed(anyString())).thenReturn(expectedVector);

        var result = embeddingService.generateEmbedding(book);

        assertThat(result).isEqualTo(expectedVector);
        String expectedInput = "Title: Test Title. Category: Fiction. Authors: , Description: Test Description";
        verify(embeddingModelAdapter).embed(expectedInput);
    }

    @Test
    void shouldGenerateEmbeddingsSuccessfully() {
        var book1 = createBook("Fiction", "Title 1", "Desc 1");
        var book2 = createBook("Science", "Title 2", "Desc 2");
        var books = List.of(book1, book2);
        List<float[]> expectedVectors = List.of(new float[]{0.1f}, new float[]{0.2f});

        when(embeddingModelAdapter.embed(anyList())).thenReturn(expectedVectors);

        var result = embeddingService.generateEmbeddings(books);

        assertThat(result).isEqualTo(expectedVectors);
        var expectedInputs = List.of(
                "Title: Title 1. Category: Fiction. Authors: , Description: Desc 1",
                "Title: Title 2. Category: Science. Authors: , Description: Desc 2");
        verify(embeddingModelAdapter).embed(expectedInputs);
    }

    @Test
    void shouldThrowExceptionWhenBookTranslationsAreNull() {
        var book = new Book();
        book.setTranslations(null);

        assertThatThrownBy(() -> embeddingService.generateEmbedding(book))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Book translations must not be null");
    }

    @Test
    void shouldThrowExceptionWhenEnglishBookTranslationIsMissing() {
        var book = new Book();
        book.setTranslations(new HashMap<>());

        assertThatThrownBy(() -> embeddingService.generateEmbedding(book))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Default translation must not be null");
    }

    @Test
    void shouldThrowExceptionWhenCategoryIsNull() {
        var bookTranslation = BookTranslation.builder().title("Title").build();
        var book = Book.builder()
                .category(null)
                .translations(Map.of("en", bookTranslation))
                .build();

        assertThatThrownBy(() -> embeddingService.generateEmbedding(book))
                .isInstanceOf(NullPointerException.class);
    }

    private Book createBook(String categoryName, String title, String description) {
        var categoryTranslation = CategoryTranslation.builder()
                .name(categoryName)
                .build();
        var category = Category.builder()
                .translations(Map.of("en", categoryTranslation))
                .build();

        var bookTranslation = BookTranslation.builder()
                .title(title)
                .description(description)
                .build();

        return Book.builder()
                .category(category)
                .translations(Map.of("en", bookTranslation))
                .build();
    }

}
