package org.example.library.recommendation.service;

import org.assertj.core.data.Offset;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.category.domain.Category;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.GenreMapping;
import org.example.library.recommendation.domain.TfIdfVocabulary;
import org.example.library.recommendation.repository.GenreMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import smile.nlp.tokenizer.SimpleTokenizer;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorizerServiceTest {

    @Mock
    private GenreMappingRepository genreMappingRepository;

    @Mock
    private SimpleTokenizer simpleTokenizer;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private VectorizerService vectorizerService;

    @Test
    void shouldCalculateCorrectVector() {
        var category = new Category();
        category.setId(1);
        var translation = new BookTranslation();
        translation.setDescription("Java Spring book");
        var book = new Book();
        book.setCategory(category);
        book.setTranslations(Map.of("en", translation));
        var vocab = Map.of(
                "java", TfIdfVocabulary.builder().vectorIndex(0).idfScore(1.5).build(),
                "spring", TfIdfVocabulary.builder().vectorIndex(1).idfScore(2.0).build());
        var mapping = GenreMapping.builder().vectorIndex(2).build();

        when(properties.getTotalVectorSize()).thenReturn(10);
        when(properties.getTextVectorSize()).thenReturn(5);
        when(properties.getGenreCoefficient()).thenReturn(2.0f);
        when(simpleTokenizer.split("java spring book")).thenReturn(new String[]{"java", "spring", "book"});
        when(genreMappingRepository.findByCategoryId(1)).thenReturn(Optional.of(mapping));

        var vector = vectorizerService.calculateVector(book, vocab);

        assertThat(vector).isNotNull();
        assertThat(vector).hasSize(10);
        assertThat(vector[0]).isCloseTo(0.5f, Offset.offset(0.001f));
        assertThat(vector[1]).isCloseTo(0.666f, Offset.offset(0.001f));
        assertThat(vector[7]).isCloseTo(2.0f, Offset.offset(0.001f));
        assertThat(vector[2]).isEqualTo(0.0f);
    }

    @Test
    void shouldReturnNullWhenTranslationsAreNull() {
        var book = new Book();
        book.setTranslations(null);

        var vector = vectorizerService.calculateVector(book, Map.of());

        assertThat(vector).isNull();
    }

    @Test
    void shouldReturnNullWhenEnglishDescriptionIsMissing() {
        var translation = new BookTranslation();
        translation.setDescription("Opis");
        var book = new Book();
        book.setTranslations(Map.of("pl", translation));

        var vector = vectorizerService.calculateVector(book, Map.of());

        assertThat(vector).isNull();
    }

    @Test
    void shouldReturnNullWhenEnglishDescriptionIsBlank() {
        var translation = new BookTranslation();
        translation.setDescription("   ");
        var book = new Book();
        book.setTranslations(Map.of("en", translation));

        var vector = vectorizerService.calculateVector(book, Map.of());

        assertThat(vector).isNull();
    }

    @Test
    void shouldCalculateVectorWithoutGenreWhenCategoryIsNull() {
        var translation = new BookTranslation();
        translation.setDescription("test description");
        var book = new Book();
        book.setTranslations(Map.of("en", translation));

        when(properties.getTotalVectorSize()).thenReturn(10);
        when(simpleTokenizer.split("test description")).thenReturn(new String[]{"test", "description"});

        var vector = vectorizerService.calculateVector(book, Map.of());

        assertThat(vector).isNotNull();
        assertThat(vector).hasSize(10);
    }

    @Test
    void shouldCalculateVectorWithoutGenreWhenGenreMappingIsNotFound() {
        var category = new Category();
        category.setId(1);
        var translation = new BookTranslation();
        translation.setDescription("test description");
        var book = new Book();
        book.setCategory(category);
        book.setTranslations(Map.of("en", translation));

        when(properties.getTotalVectorSize()).thenReturn(10);
        when(simpleTokenizer.split("test description")).thenReturn(new String[]{"test", "description"});
        when(genreMappingRepository.findByCategoryId(1)).thenReturn(Optional.empty());

        var vector = vectorizerService.calculateVector(book, Map.of());

        assertThat(vector).isNotNull();
        assertThat(vector).hasSize(10);
    }

}