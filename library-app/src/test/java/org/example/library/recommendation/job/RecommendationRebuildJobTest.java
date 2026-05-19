package org.example.library.recommendation.job;

import org.example.library.recommendation.service.GlobalRebuildService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecommendationRebuildJobTest {

    @Mock
    private GlobalRebuildService rebuildService;

    @Mock
    private RecommendationRebuildJob self;

    private RecommendationRebuildJob job;


    @BeforeEach
    void setUp() {
        job = new RecommendationRebuildJob(rebuildService, self);
    }

    @Test
    void shouldCallRunOnStartup() {
        job.onStartup();

        verify(self).run();
        verifyNoInteractions(rebuildService);
    }

    @Test
    void shouldCallExecuteFullRebuildWhenRun() {
        job.run();

        verify(rebuildService).executeFullRebuild();
    }

}
