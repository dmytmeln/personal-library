package org.example.library.recommendation.job;

import lombok.extern.slf4j.Slf4j;
import org.example.library.recommendation.service.BookEmbeddingBackfillService;
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
public class BookEmbeddingBackfillJob {

    private final BookEmbeddingBackfillService embeddingBackfillService;
    private final BookEmbeddingBackfillJob self;

    public BookEmbeddingBackfillJob(BookEmbeddingBackfillService embeddingBackfillService,
                                    @Lazy BookEmbeddingBackfillJob self) {
        this.embeddingBackfillService = embeddingBackfillService;
        this.self = self;
    }


    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        self.setBookEmbeddings();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void setBookEmbeddings() {
        log.info("Starting scheduled recommendation model update...");
        embeddingBackfillService.backfillEmbeddings();
        log.info("Scheduled recommendation model update completed.");
    }

}
