package org.example.library.recommendation.service;

import org.example.library.category.repository.CategoryRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.GenreMapping;
import org.example.library.recommendation.repository.GenreMappingRepository;
import org.example.library.recommendation.service.GenreMappingService.GenreChangeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenreMappingServiceTest {

    @Mock
    private GenreMappingRepository genreMappingRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RecommendationProperties properties;

    @InjectMocks
    private GenreMappingService genreMappingService;

    @Captor
    private ArgumentCaptor<Collection<GenreMapping>> genreMappingCollectionCaptor;


    @Test
    void shouldReturnRebuildWhenCategoriesDeleted() {
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2));
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(1, 2, 3));

        var result = genreMappingService.getGenreChangeType();

        assertThat(result).isEqualTo(GenreChangeType.REBUILD);
    }

    @Test
    void shouldReturnAppendWhenCategoriesAdded() {
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2, 3));
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(1, 2));

        var result = genreMappingService.getGenreChangeType();

        assertThat(result).isEqualTo(GenreChangeType.APPEND);
    }

    @Test
    void shouldReturnNoneWhenCategoriesMatch() {
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2, 3));
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(3, 2, 1));

        var result = genreMappingService.getGenreChangeType();

        assertThat(result).isEqualTo(GenreChangeType.NONE);
    }

    @Test
    void shouldAppendNewCategoriesCorrectly() {
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(1, 2));
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2, 3, 4));
        when(genreMappingRepository.count()).thenReturn(2L);
        when(properties.getGenreVectorSize()).thenReturn(10);
        when(genreMappingRepository.findMaxVectorIndex()).thenReturn(Optional.of(1));

        genreMappingService.appendNewCategories();

        verify(genreMappingRepository).saveAll(genreMappingCollectionCaptor.capture());
        var savedMappings = genreMappingCollectionCaptor.getValue();
        assertThat(savedMappings).hasSize(2);
        assertThat(savedMappings).extracting(GenreMapping::getCategoryId).containsExactlyInAnyOrder(3, 4);
        assertThat(savedMappings).extracting(GenreMapping::getVectorIndex).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void shouldNotAppendWhenNoNewCategories() {
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(1, 2));
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2));

        genreMappingService.appendNewCategories();

        verify(genreMappingRepository).findAllCategoryIds();
        verifyNoMoreInteractions(genreMappingRepository);
        verifyNoInteractions(properties);
    }

    @Test
    void shouldLimitNewCategoriesWhenExceedingMaxVectorSize() {
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(1, 2));
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(genreMappingRepository.count()).thenReturn(2L);
        when(properties.getGenreVectorSize()).thenReturn(3);
        when(genreMappingRepository.findMaxVectorIndex()).thenReturn(Optional.of(1));

        genreMappingService.appendNewCategories();

        verify(genreMappingRepository).saveAll(genreMappingCollectionCaptor.capture());
        var savedMappings = genreMappingCollectionCaptor.getValue();
        assertThat(savedMappings).hasSize(1);
        assertThat(savedMappings).extracting(GenreMapping::getCategoryId).containsAnyOf(3, 4, 5);
        assertThat(savedMappings).extracting(GenreMapping::getVectorIndex).containsExactly(2);
    }

    @Test
    void shouldNotAppendIfLimitAlreadyReached() {
        when(genreMappingRepository.findAllCategoryIds()).thenReturn(Set.of(1, 2));
        when(categoryRepository.findAllIds()).thenReturn(Set.of(1, 2, 3));
        when(genreMappingRepository.count()).thenReturn(2L);
        when(properties.getGenreVectorSize()).thenReturn(2);

        genreMappingService.appendNewCategories();

        verify(genreMappingRepository).count();
        verifyNoMoreInteractions(genreMappingRepository);
    }

    @Test
    void shouldRebuildMappingCorrectly() {
        when(categoryRepository.findAllIds()).thenReturn(Set.of(10, 20, 30));
        when(properties.getGenreVectorSize()).thenReturn(10);

        genreMappingService.rebuildGenreMapping();

        verify(genreMappingRepository).deleteAllInBatch();
        verify(genreMappingRepository).saveAll(genreMappingCollectionCaptor.capture());
        var savedMappings = genreMappingCollectionCaptor.getValue();

        assertThat(savedMappings).hasSize(3);
        assertThat(savedMappings).extracting(GenreMapping::getCategoryId).containsExactlyInAnyOrder(10, 20, 30);
        assertThat(savedMappings).extracting(GenreMapping::getVectorIndex).containsExactlyInAnyOrder(0, 1, 2);
    }

    @Test
    void shouldLimitRebuildWhenExceedingMaxVectorSize() {
        when(categoryRepository.findAllIds()).thenReturn(Set.of(10, 20, 30, 40));
        when(properties.getGenreVectorSize()).thenReturn(2);

        genreMappingService.rebuildGenreMapping();

        verify(genreMappingRepository).deleteAllInBatch();
        verify(genreMappingRepository).saveAll(genreMappingCollectionCaptor.capture());
        var savedMappings = genreMappingCollectionCaptor.getValue();

        assertThat(savedMappings).hasSize(2);
        assertThat(savedMappings).extracting(GenreMapping::getVectorIndex).containsExactlyInAnyOrder(0, 1);
    }

}