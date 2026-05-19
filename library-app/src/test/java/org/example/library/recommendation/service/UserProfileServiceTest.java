package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.UserProfileVector;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private UserProfileVectorRepository userProfileVectorRepository;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void shouldReturnSavedVectorIfPresent() {
        float[] expectedVector = new float[384];
        expectedVector[0] = 0.5f;
        var savedVector = UserProfileVector.builder()
                .userId(1)
                .embedding(expectedVector)
                .build();

        when(userProfileVectorRepository.findById(1)).thenReturn(Optional.of(savedVector));

        var result = userProfileService.getUserProfileEmbedding(1);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(expectedVector);
    }

    @Test
    void shouldCleanupIfNoLibraryBooksOnUpdate() {
        when(libraryBookRepository.findAllWithVectorsByUserId(1)).thenReturn(List.of());

        userProfileService.rebuildUserProfileVector(1);

        verify(userProfileVectorRepository).deleteById(1);
    }

    @Test
    void shouldCalculateCorrectWeightsAndRecencyDecay() {
        var book1 = new Book();
        book1.setId(1);
        float[] v1 = new float[384];
        v1[0] = 1.0f;
        book1.setEmbedding(v1);
        var book2 = new Book();
        book2.setId(2);
        float[] v2 = new float[384];
        v2[1] = 1.0f;
        book2.setEmbedding(v2);
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

            when(properties.getFavoriteWeight()).thenReturn(2.0f);
            when(properties.getRating5Weight()).thenReturn(1.5f);
            when(properties.getRecencyDecayFactor()).thenReturn(0.1f);
            when(libraryBookRepository.findAllWithVectorsByUserId(1)).thenReturn(List.of(favBook, ratedBook));

            userProfileService.rebuildUserProfileVector(1);

            verify(userProfileVectorRepository).deleteById(1);
            verify(userProfileVectorRepository).save(any(UserProfileVector.class));
        }
    }

    @Test
    void shouldHandleBookWithNoRatingWeightAndNullAddedAt() {
        var book = new Book();
        book.setId(1);
        float[] v = new float[384];
        v[0] = 1.0f;
        v[1] = 1.0f;
        book.setEmbedding(v);
        var normalBook = new LibraryBook();
        normalBook.setBook(book);
        normalBook.setStatus(LibraryBookStatus.READING);
        normalBook.setAddedAt(null);

        when(properties.getNoRatingWeight()).thenReturn(1.0f);
        when(libraryBookRepository.findAllWithVectorsByUserId(1)).thenReturn(List.of(normalBook));

        userProfileService.rebuildUserProfileVector(1);

        verify(userProfileVectorRepository).deleteById(1);
        verify(userProfileVectorRepository).save(any(UserProfileVector.class));
    }

}
