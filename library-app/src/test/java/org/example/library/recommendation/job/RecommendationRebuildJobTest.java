package org.example.library.recommendation.job;

import org.example.library.recommendation.service.GenreMappingService;
import org.example.library.recommendation.service.GenreMappingService.GenreChangeType;
import org.example.library.recommendation.service.GlobalRebuildService;
import org.example.library.recommendation.service.RecommendationTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationRebuildJobTest {

    @Mock
    private RecommendationTriggerService triggerService;

    @Mock
    private GenreMappingService genreMappingService;

    @Mock
    private GlobalRebuildService rebuildService;

    @Mock
    private RecommendationRebuildJob self;

    private RecommendationRebuildJob job;


    @BeforeEach
    void setUp() {
        job = new RecommendationRebuildJob(triggerService, genreMappingService, rebuildService, self);
    }

    @Test
    void shouldCallRunOnStartup() {
        job.onStartup();

        verify(self).run();
        verifyNoInteractions(triggerService, genreMappingService, rebuildService);
    }

    @Test
    void shouldRebuildEverythingWhenVocabRebuildAndGenreRebuild() {
        when(triggerService.shouldRebuildVocabulary()).thenReturn(true);
        when(triggerService.getGenreChangeType()).thenReturn(GenreChangeType.REBUILD);

        job.run();

        verify(genreMappingService).rebuildGenreMapping();
        verify(rebuildService).executeFullRebuild();
        verifyNoMoreInteractions(genreMappingService);
    }

    @Test
    void shouldRebuildVocabAndAppendGenresWhenVocabRebuildAndGenreAppend() {
        when(triggerService.shouldRebuildVocabulary()).thenReturn(true);
        when(triggerService.getGenreChangeType()).thenReturn(GenreChangeType.APPEND);

        job.run();

        verify(genreMappingService).appendNewCategories();
        verify(rebuildService).executeFullRebuild();
        verifyNoMoreInteractions(genreMappingService);
    }

    @Test
    void shouldRebuildVocabOnlyWhenVocabRebuildAndGenreNone() {
        when(triggerService.shouldRebuildVocabulary()).thenReturn(true);
        when(triggerService.getGenreChangeType()).thenReturn(GenreChangeType.NONE);

        job.run();

        verify(rebuildService).executeFullRebuild();
        verifyNoInteractions(genreMappingService);
    }

    @Test
    void shouldAppendCategoriesWhenVocabNotRebuildAndGenreChanged() {
        when(triggerService.shouldRebuildVocabulary()).thenReturn(false);
        when(triggerService.getGenreChangeType()).thenReturn(GenreChangeType.APPEND);

        job.run();

        verify(genreMappingService).appendNewCategories();
        verifyNoInteractions(rebuildService);
    }

    @Test
    void shouldAppendCategoriesWhenVocabNotRebuildAndGenreRebuild() {
        when(triggerService.shouldRebuildVocabulary()).thenReturn(false);
        when(triggerService.getGenreChangeType()).thenReturn(GenreChangeType.REBUILD);

        job.run();

        verify(genreMappingService).appendNewCategories();
        verifyNoInteractions(rebuildService);
    }

    @Test
    void shouldDoNothingWhenModelIsUpToDate() {
        when(triggerService.shouldRebuildVocabulary()).thenReturn(false);
        when(triggerService.getGenreChangeType()).thenReturn(GenreChangeType.NONE);

        job.run();

        verifyNoInteractions(genreMappingService, rebuildService);
    }

}