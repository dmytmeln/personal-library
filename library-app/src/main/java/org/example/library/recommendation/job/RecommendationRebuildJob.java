package org.example.library.recommendation.job;

import lombok.extern.slf4j.Slf4j;
import org.example.library.recommendation.service.GenreMappingService;
import org.example.library.recommendation.service.GenreMappingService.GenreChangeType;
import org.example.library.recommendation.service.GlobalRebuildService;
import org.example.library.recommendation.service.RecommendationTriggerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.recommendations.enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationRebuildJob {

    private final RecommendationTriggerService triggerService;
    private final GenreMappingService genreMappingService;
    private final GlobalRebuildService rebuildService;
    private final RecommendationRebuildJob self;

    public RecommendationRebuildJob(RecommendationTriggerService triggerService,
                                    GenreMappingService genreMappingService,
                                    GlobalRebuildService rebuildService,
                                    @Lazy RecommendationRebuildJob self) {
        this.triggerService = triggerService;
        this.genreMappingService = genreMappingService;
        this.rebuildService = rebuildService;
        this.self = self;
    }


    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        self.run();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        boolean shouldRebuildVocab = triggerService.shouldRebuildVocabulary();
        var genreChangeType = triggerService.getGenreChangeType();

        if (shouldRebuildVocab) {
            if (GenreChangeType.REBUILD == genreChangeType) {
                genreMappingService.rebuildGenreMapping();
            } else if (GenreChangeType.APPEND == genreChangeType) {
                genreMappingService.appendNewCategories();
            }
            log.info("Starting full rebuild of recommendation model...");
            rebuildService.executeFullRebuild();
        } else if (GenreChangeType.NONE != genreChangeType) {
            log.info("Incremental genre mapping update...");
            genreMappingService.appendNewCategories();
        } else {
            log.info("Recommendation model is up to date.");
        }
    }

}
