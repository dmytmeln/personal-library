package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.UserProfileVector;
import org.example.library.recommendation.event.UserProfileUpdatedEvent;
import org.example.library.recommendation.repository.UserProfileVectorRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final LibraryBookRepository libraryBookRepository;
    private final UserProfileVectorRepository userProfileVectorRepository;
    private final VocabularyMetadataService vocabularyMetadataService;
    private final RecommendationProperties properties;


    @Transactional
    public float[] calculateUserProfileVector(Integer userId) {
        var metadata = vocabularyMetadataService.getMetadata();
        int currentVersion = metadata.getCurrentVersion();

        var storedVector = userProfileVectorRepository.findById(userId);

        if (storedVector.isPresent() && storedVector.get().getVersion() == currentVersion) {
            log.debug("Using saved user profile vector for user {} (version {})", userId, currentVersion);
            return storedVector.get().getVector();
        }

        log.info("Stored vector for user {} is missing or outdated. Recalculating...", userId);
        return calculateAndSaveVector(userId, currentVersion);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileUpdated(UserProfileUpdatedEvent event) {
        log.info("Asynchronously rebuilding user profile vector for user {}", event.userId());
        var metadata = vocabularyMetadataService.getMetadata();
        calculateAndSaveVector(event.userId(), metadata.getCurrentVersion());
    }

    private float[] calculateAndSaveVector(Integer userId, int version) {
        float[] vector = performCalculation(userId);

        if (vector != null) {
            var userProfileVector = UserProfileVector.builder()
                    .userId(userId)
                    .vector(vector)
                    .version(version)
                    .updatedAt(LocalDateTime.now())
                    .build();
            userProfileVectorRepository.save(userProfileVector);
            log.debug("Saved user profile vector for user {}", userId);
        } else {
            userProfileVectorRepository.deleteById(userId);
            log.debug("Deleted user profile vector for user {} due to no valid data", userId);
        }

        return vector;
    }

    private float[] performCalculation(Integer userId) {
        var userLibrary = libraryBookRepository.findAllWithVectorsByUserId(userId);

        if (userLibrary.isEmpty()) {
            log.warn("User {} has no books with vectors. Returning null.", userId);
            return null;
        }

        int vectorSize = properties.getTotalVectorSize();
        var combinedVector = new float[vectorSize];
        float totalWeight = 0.0f;

        for (LibraryBook lb : userLibrary) {
            float weight = determineWeight(lb);
            float recencyFactor = calculateRecencyFactor(lb.getAddedAt());
            float finalWeight = weight * recencyFactor;

            var bookVector = lb.getBook().getDescriptionVector();

            if (bookVector == null || bookVector.length != vectorSize) continue;

            for (int i = 0; i < vectorSize; i++) {
                combinedVector[i] += bookVector[i] * finalWeight;
            }
            totalWeight += Math.abs(finalWeight);
        }

        if (totalWeight > 0) {
            for (int i = 0; i < vectorSize; i++) {
                combinedVector[i] /= totalWeight;
            }
        }

        return combinedVector;
    }

    private float calculateRecencyFactor(LocalDateTime addedAt) {
        if (addedAt == null) return 1.0f;

        long monthsPassed = ChronoUnit.MONTHS.between(addedAt, LocalDateTime.now());
        return (float) Math.exp(-properties.getRecencyDecayFactor() * monthsPassed);
    }

    private float determineWeight(LibraryBook lb) {
        if (LibraryBookStatus.FAVORITE == lb.getStatus()) return properties.getFavoriteWeight();

        Byte rating = lb.getRating();
        if (rating == null) return properties.getNoRatingWeight();

        return switch (rating) {
            case 5 -> properties.getRating5Weight();
            case 4 -> properties.getRating4Weight();
            case 3 -> properties.getRating3Weight();
            default -> properties.getLowRatingWeight();
        };
    }

}
