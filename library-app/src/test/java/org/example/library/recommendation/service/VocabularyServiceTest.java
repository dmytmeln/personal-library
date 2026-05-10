package org.example.library.recommendation.service;

import org.assertj.core.data.Offset;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.TfIdfVocabulary;
import org.example.library.recommendation.repository.TfIdfVocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import smile.nlp.tokenizer.SimpleTokenizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    @Mock
    private TfIdfVocabularyRepository vocabularyRepository;

    @Mock
    private SimpleTokenizer simpleTokenizer;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private VocabularyService vocabularyService;

    @Captor
    private ArgumentCaptor<Collection<TfIdfVocabulary>> vocabularyCollectionCaptor;


    @Test
    void shouldCalculateNewIdfCorrectly() {
        var descriptions = List.of(
                "Java is a programming language",
                "Spring is a framework for Java",
                "Java and Spring are popular");

        when(properties.getTextVectorSize()).thenReturn(10);
        when(simpleTokenizer.split("java is a programming language")).thenReturn(new String[]{"java", "is", "a", "programming", "language"});
        when(simpleTokenizer.split("spring is a framework for java")).thenReturn(new String[]{"spring", "is", "a", "framework", "for", "java"});
        when(simpleTokenizer.split("java and spring are popular")).thenReturn(new String[]{"java", "and", "spring", "are", "popular"});

        var idfMap = vocabularyService.calculateNewIdf(descriptions);

        assertThat(idfMap).isNotNull();
        assertThat(idfMap).containsKey("java");
        assertThat(idfMap.get("java")).isCloseTo(0.0, Offset.offset(0.001));
        assertThat(idfMap).containsKey("spring");
        assertThat(idfMap.get("spring")).isCloseTo(0.176, Offset.offset(0.001));
        assertThat(idfMap).containsKey("programming");
        assertThat(idfMap.get("programming")).isCloseTo(0.477, Offset.offset(0.001));
        assertThat(idfMap).doesNotContainKey("is");
        assertThat(idfMap).doesNotContainKey("a");
        assertThat(idfMap).doesNotContainKey("and");
    }

    @Test
    void shouldSkipNullAndBlankDescriptions() {
        var descriptions = new ArrayList<String>();
        descriptions.add("valid description");
        descriptions.add(null);
        descriptions.add("   ");

        when(properties.getTextVectorSize()).thenReturn(5);
        when(simpleTokenizer.split("valid description")).thenReturn(new String[]{"valid", "description"});

        var idfMap = vocabularyService.calculateNewIdf(descriptions);

        assertThat(idfMap).isNotNull();
        assertThat(idfMap).hasSize(2);
        assertThat(idfMap).containsKey("valid");
        assertThat(idfMap).containsKey("description");
    }

    @Test
    void shouldLimitVocabularySizeBasedOnProperties() {
        var descriptions = List.of("word1 word2 word3 word4 word5");

        when(properties.getTextVectorSize()).thenReturn(2);
        when(simpleTokenizer.split("word1 word2 word3 word4 word5")).thenReturn(new String[]{"word1", "word2", "word3", "word4", "word5"});

        var idfMap = vocabularyService.calculateNewIdf(descriptions);

        assertThat(idfMap).hasSize(2);
    }

    @Test
    void shouldSaveVocabularyCorrectly() {
        var idfMap = Map.of("java", 0.0, "spring", 0.176);
        var version = 1;

        when(vocabularyRepository.saveAll(anyCollection())).thenAnswer(invocation -> new ArrayList<>((Collection<TfIdfVocabulary>) invocation.getArgument(0)));

        var savedMap = vocabularyService.saveVocabularyForVersion(idfMap, version);

        assertThat(savedMap).isNotNull();
        assertThat(savedMap).hasSize(2);
        verify(vocabularyRepository).saveAll(vocabularyCollectionCaptor.capture());
        var savedCollection = vocabularyCollectionCaptor.getValue();
        assertThat(savedCollection).hasSize(2);
        assertThat(savedCollection).extracting(TfIdfVocabulary::getVersion).containsOnly(version);
        assertThat(savedCollection).extracting(TfIdfVocabulary::getVectorIndex).containsExactlyInAnyOrder(0, 1);
    }

    @Test
    void shouldThrowExceptionIfSaveMismatch() {
        var idfMap = Map.of("java", 0.0);
        var version = 1;

        when(vocabularyRepository.saveAll(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> vocabularyService.saveVocabularyForVersion(idfMap, version))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Vocabulary save mismatch");
    }

}