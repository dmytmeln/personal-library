package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.UserProfileVector;
import org.example.library.recommendation.repository.UserProfileVectorRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private static final int EMBEDDING_SIZE = 384;

    private final LibraryBookRepository libraryBookRepository;
    private final UserProfileVectorRepository userProfileVectorRepository;
    private final RecommendationProperties properties;

    @Transactional(readOnly = true)
    public Optional<float[]> getUserProfileEmbedding(Integer userId) {
        log.debug("Using saved user profile vector for user {}", userId);

        return userProfileVectorRepository.findById(userId)
                .map(UserProfileVector::getEmbedding);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildUserProfileVector(Integer userId) {
        log.info("Rebuilding user profile vector for user {}", userId);
        userProfileVectorRepository.deleteById(userId);

        List<LibraryBook> userLibrary = libraryBookRepository.findAllWithVectorsByUserId(userId);
        if (userLibrary.isEmpty()) {
            log.debug("No books found for user {}, skipping profile rebuild", userId);
            return;
        }

        float[] vector = calculateUserProfileVector(userLibrary);
        var userProfileVector = UserProfileVector.builder()
                .userId(userId)
                .embedding(vector)
                .updatedAt(LocalDateTime.now())
                .build();

        userProfileVectorRepository.save(userProfileVector);
        log.debug("Persisted profile vector for user {}", userId);
    }

    private float[] calculateUserProfileVector(List<LibraryBook> userLibrary) {
        float[] accumulatedVector = new float[EMBEDDING_SIZE];
        float totalWeight = 0.0f;

        for (LibraryBook lb : userLibrary) {
            float[] bookEmbedding = lb.getBook().getEmbedding();
            if (bookEmbedding.length != EMBEDDING_SIZE) {
                log.error("Invalid book embedding length for book {}. Skipping.", lb.getBook().getId());
                continue;
            }
            float bookWeight = calculateBookWeight(lb);

            accumulateBookWeightedEmbedding(accumulatedVector, bookEmbedding, bookWeight);

            totalWeight += Math.abs(bookWeight);
        }

        return computeWeightedAverageVector(accumulatedVector, totalWeight);
    }

    private void accumulateBookWeightedEmbedding(float[] accumulatedVector, float[] bookEmbedding, float bookWeight) {
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            float bookWeightedEmbeddingDimension = bookEmbedding[i] * bookWeight;
            accumulatedVector[i] += bookWeightedEmbeddingDimension;
        }
    }

    private float[] computeWeightedAverageVector(float[] userProfileVector, float totalWeight) {
        if (totalWeight <= 0) {
            return userProfileVector;
        }

        float[] weightedAverage = new float[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            weightedAverage[i] = userProfileVector[i] / totalWeight;
        }

        return weightedAverage;
    }

    private float calculateBookWeight(LibraryBook lb) {
        float weight = determineBookWeight(lb.getStatus(), lb.getRating());
        float recencyFactor = calculateRecencyFactor(lb.getAddedAt());

        return weight * recencyFactor;
    }

    private float calculateRecencyFactor(LocalDateTime addedAt) {
        if (addedAt == null) {
            return 1.0f;
        }

        long monthsSinceAddingPassed = ChronoUnit.MONTHS.between(addedAt, LocalDateTime.now());
        return (float) Math.exp(-properties.getRecencyDecayFactor() * monthsSinceAddingPassed);
    }

    private float determineBookWeight(LibraryBookStatus libraryBookStatus, Byte rating) {
        if (LibraryBookStatus.FAVORITE == libraryBookStatus) {
            return properties.getFavoriteWeight();
        }

        if (rating == null) {
            return properties.getNoRatingWeight();
        }
        return switch (rating) {
            case 5 -> properties.getRating5Weight();
            case 4 -> properties.getRating4Weight();
            case 3 -> properties.getRating3Weight();
            default -> properties.getLowRatingWeight();
        };
    }

}
