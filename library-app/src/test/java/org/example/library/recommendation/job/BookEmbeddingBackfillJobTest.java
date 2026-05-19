package org.example.library.recommendation.job;

import org.example.library.recommendation.service.BookEmbeddingBackfillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookEmbeddingBackfillJobTest {

    @Mock
    private BookEmbeddingBackfillService rebuildService;

    @Mock
    private BookEmbeddingBackfillJob self;

    private BookEmbeddingBackfillJob job;


    @BeforeEach
    void setUp() {
        job = new BookEmbeddingBackfillJob(rebuildService, self);
    }

    @Test
    void shouldCallSetBookEmbeddingsOnStartup() {
        job.onStartup();

        verify(self).setBookEmbeddings();
        verifyNoInteractions(rebuildService);
    }

    @Test
    void shouldCallExecuteFullRebuildWhenSetBookEmbeddings() {
        job.setBookEmbeddings();

        verify(rebuildService).backfillEmbeddings();
    }

}
