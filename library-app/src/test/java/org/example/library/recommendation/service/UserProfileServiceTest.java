package org.example.library.recommendation.service;

import org.assertj.core.data.Offset;
import org.example.library.book.domain.Book;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.UserProfileVector;
import org.example.library.recommendation.domain.VocabularyMetadata;
import org.example.library.recommendation.repository.UserProfileVectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private UserProfileVectorRepository userProfileVectorRepository;

    @Mock
    private VocabularyMetadataService vocabularyMetadataService;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private UserProfileService userProfileService;


    @Test
    void shouldReturnSavedVectorIfVersionMatches() {
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(2);
        float[] expectedVector = {0.5f, 0.5f};
        var savedVector = UserProfileVector.builder()
                .userId(1)
                .version(2)
                .vector(expectedVector)
                .build();

        when(vocabularyMetadataService.getMetadata()).thenReturn(metadata);
        when(userProfileVectorRepository.findById(1)).thenReturn(Optional.of(savedVector));

        var result = userProfileService.calculateUserProfileVector(1);

        assertThat(result).isSameAs(expectedVector);
        verifyNoInteractions(libraryBookRepository);
    }

    @Test
    void shouldReturnNullAndCleanupIfNoLibraryBooks() {
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(1);

        when(vocabularyMetadataService.getMetadata()).thenReturn(metadata);
        when(userProfileVectorRepository.findById(1)).thenReturn(Optional.empty());
        when(libraryBookRepository.findAllWithVectorsByUserId(1)).thenReturn(List.of());

        var result = userProfileService.calculateUserProfileVector(1);

        assertThat(result).isNull();
        verify(userProfileVectorRepository).deleteById(1);
    }

    @Test
    void shouldCalculateCorrectWeightsAndRecencyDecay() {
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(1);
        var book1 = new Book();
        book1.setDescriptionVector(new float[]{1.0f, 0.0f, 0.0f});
        var book2 = new Book();
        book2.setDescriptionVector(new float[]{0.0f, 1.0f, 0.0f});
        var fixedNow = LocalDateTime.of(2024, 1, 1, 12, 0);

        try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedTime.when(LocalDateTime::now).thenReturn(fixedNow);

            var favBook = new LibraryBook();
            favBook.setBook(book1);
            favBook.setStatus(LibraryBookStatus.FAVORITE);
            favBook.setAddedAt(fixedNow);
            var ratedBook = new LibraryBook();
            ratedBook.setBook(book2);
            ratedBook.setRating((byte) 5);
            ratedBook.setAddedAt(fixedNow.minusMonths(5));

            when(vocabularyMetadataService.getMetadata()).thenReturn(metadata);
            when(userProfileVectorRepository.findById(1)).thenReturn(Optional.empty());
            when(properties.getTotalVectorSize()).thenReturn(3);
            when(properties.getFavoriteWeight()).thenReturn(2.0f);
            when(properties.getRating5Weight()).thenReturn(1.5f);
            when(properties.getRecencyDecayFactor()).thenReturn(0.1f);
            when(libraryBookRepository.findAllWithVectorsByUserId(1)).thenReturn(List.of(favBook, ratedBook));

            var result = userProfileService.calculateUserProfileVector(1);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result[0]).isCloseTo(0.687f, Offset.offset(0.01f));
            assertThat(result[1]).isCloseTo(0.312f, Offset.offset(0.01f));
            assertThat(result[2]).isEqualTo(0.0f);
            verify(userProfileVectorRepository).save(any(UserProfileVector.class));
        }
    }

    @Test
    void shouldHandleBookWithNoRatingWeightAndNullAddedAt() {
        var metadata = new VocabularyMetadata();
        metadata.setCurrentVersion(1);
        var book = new Book();
        book.setDescriptionVector(new float[]{1.0f, 1.0f});
        var normalBook = new LibraryBook();
        normalBook.setBook(book);
        normalBook.setStatus(LibraryBookStatus.READING);
        normalBook.setAddedAt(null);

        when(vocabularyMetadataService.getMetadata()).thenReturn(metadata);
        when(userProfileVectorRepository.findById(1)).thenReturn(Optional.empty());
        when(properties.getTotalVectorSize()).thenReturn(2);
        when(properties.getNoRatingWeight()).thenReturn(1.0f);
        when(libraryBookRepository.findAllWithVectorsByUserId(1)).thenReturn(List.of(normalBook));

        var result = userProfileService.calculateUserProfileVector(1);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo(1.0f);
        assertThat(result[1]).isEqualTo(1.0f);
        verify(userProfileVectorRepository).save(any(UserProfileVector.class));
    }

}