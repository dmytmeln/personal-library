package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationTriggerService {

    private final BookRepository bookRepository;
    private final VocabularyMetadataService metadataService;
    private final GenreMappingService genreMappingService;

    private int countThreshold;
    private int daysThreshold;


    @Value("${recommendations.trigger.count:50}")
    public void setCountThreshold(int countThreshold) {
        this.countThreshold = countThreshold;
    }

    @Value("${recommendations.trigger.days:14}")
    public void setDaysThreshold(int daysThreshold) {
        this.daysThreshold = daysThreshold;
    }

    @Transactional
    public boolean shouldRebuildVocabulary() {
        var metadata = metadataService.getMetadata();

        long preliminaryBooks = bookRepository.countWhereBookStatusNot(BookStatus.SYNCED);
        if (preliminaryBooks >= countThreshold) {
            log.info("Trigger: Found {} new books. Threshold {} exceeded.", preliminaryBooks, countThreshold);
            return true;
        }

        var lastUpdate = metadata.getLastRebuildAt();
        if (lastUpdate != null && lastUpdate.isBefore(LocalDateTime.now().minusDays(daysThreshold))) {
            log.info("Trigger: Last update was {}. Threshold {} days exceeded.", lastUpdate, daysThreshold);
            return true;
        }

        var currentVersion = metadata.getCurrentVersion();
        if (bookRepository.countBooksWithOldVersion(currentVersion) > 0) {
            log.info("Trigger: Found books with outdated vector version. Reprocessing needed.");
            return true;
        }

        return false;
    }

    @Transactional(readOnly = true)
    public GenreMappingService.GenreChangeType getGenreChangeType() {
        return genreMappingService.getGenreChangeType();
    }

}
