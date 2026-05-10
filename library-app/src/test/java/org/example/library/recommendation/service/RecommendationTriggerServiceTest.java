package org.example.library.recommendation.service;

import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.example.library.recommendation.domain.VocabularyMetadata;
import org.example.library.recommendation.service.GenreMappingService.GenreChangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationTriggerServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private VocabularyMetadataService metadataService;

    @Mock
    private GenreMappingService genreMappingService;

    @InjectMocks
    private RecommendationTriggerService triggerService;


    @BeforeEach
    void setUp() {
        triggerService.setCountThreshold(50);
        triggerService.setDaysThreshold(14);
    }

    @Test
    void shouldReturnTrueWhenPreliminaryBooksExceedThreshold() {
        var metadata = new VocabularyMetadata();
        when(metadataService.getMetadata()).thenReturn(metadata);
        when(bookRepository.countWhereBookStatusNot(BookStatus.SYNCED)).thenReturn(50L);

        var result = triggerService.shouldRebuildVocabulary();

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenLastUpdateExceedsDaysThreshold() {
        var fixedNow = LocalDateTime.of(2024, 5, 15, 12, 0);
        var metadata = new VocabularyMetadata();
        // 15 days ago, threshold is 14
        metadata.setLastRebuildAt(fixedNow.minusDays(15));
        
        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedTime.when(LocalDateTime::now).thenReturn(fixedNow);

            when(metadataService.getMetadata()).thenReturn(metadata);
            when(bookRepository.countWhereBookStatusNot(BookStatus.SYNCED)).thenReturn(10L);

            var result = triggerService.shouldRebuildVocabulary();

            assertThat(result).isTrue();
        }
    }

    @Test
    void shouldReturnTrueWhenBooksWithOldVersionExist() {
        var fixedNow = LocalDateTime.of(2024, 5, 15, 12, 0);
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(2);
        // 5 days ago, threshold not exceeded
        metadata.setLastRebuildAt(fixedNow.minusDays(5));
        
        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedTime.when(LocalDateTime::now).thenReturn(fixedNow);

            when(metadataService.getMetadata()).thenReturn(metadata);
            when(bookRepository.countWhereBookStatusNot(BookStatus.SYNCED)).thenReturn(10L);
            when(bookRepository.countBooksWithOldVersion(2)).thenReturn(5L);

            var result = triggerService.shouldRebuildVocabulary();

            assertThat(result).isTrue();
        }
    }

    @Test
    void shouldReturnFalseWhenNoTriggerConditionsMet() {
        var fixedNow = LocalDateTime.of(2024, 5, 15, 12, 0);
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(2);
        // 5 days ago, threshold not exceeded
        metadata.setLastRebuildAt(fixedNow.minusDays(5));

        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedTime.when(LocalDateTime::now).thenReturn(fixedNow);

            when(metadataService.getMetadata()).thenReturn(metadata);
            when(bookRepository.countWhereBookStatusNot(BookStatus.SYNCED)).thenReturn(10L);
            when(bookRepository.countBooksWithOldVersion(2)).thenReturn(0L);

            var result = triggerService.shouldRebuildVocabulary();

            assertThat(result).isFalse();
        }
    }

    @Test
    void shouldReturnFalseWhenLastUpdateIsNullAndOtherConditionsNotMet() {
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(2);
        metadata.setLastRebuildAt(null);

        when(metadataService.getMetadata()).thenReturn(metadata);
        when(bookRepository.countWhereBookStatusNot(BookStatus.SYNCED)).thenReturn(10L);
        when(bookRepository.countBooksWithOldVersion(2)).thenReturn(0L);

        var result = triggerService.shouldRebuildVocabulary();

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnGenreChangeTypeFromGenreMappingService() {
        when(genreMappingService.getGenreChangeType()).thenReturn(GenreChangeType.APPEND);

        var result = triggerService.getGenreChangeType();

        assertThat(result).isEqualTo(GenreChangeType.APPEND);
        verifyNoInteractions(metadataService, bookRepository);
    }

}