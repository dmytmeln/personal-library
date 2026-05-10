package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.GenreMapping;
import org.example.library.recommendation.repository.GenreMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenreMappingService {

    public enum GenreChangeType {
        NONE, APPEND, REBUILD
    }


    private final GenreMappingRepository genreMappingRepository;
    private final CategoryRepository categoryRepository;
    private final RecommendationProperties properties;


    @Transactional(readOnly = true)
    public GenreChangeType getGenreChangeType() {
        var categoryIdsInDb = categoryRepository.findAllIds();
        var categoryIdsInMapping = genreMappingRepository.findAllCategoryIds();

        boolean hasDeletions = !categoryIdsInDb.containsAll(categoryIdsInMapping);
        if (hasDeletions) {
            return GenreChangeType.REBUILD;
        }

        boolean hasAdditions = !categoryIdsInMapping.containsAll(categoryIdsInDb);
        if (hasAdditions) {
            return GenreChangeType.APPEND;
        }

        return GenreChangeType.NONE;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendNewCategories() {
        var existingCategoryIds = genreMappingRepository.findAllCategoryIds();
        var newCategoryIds = categoryRepository.findAllIds().stream()
                .filter(id -> !existingCategoryIds.contains(id))
                .toList();

        if (newCategoryIds.isEmpty()) {
            log.info("No new categories to add to genre mapping.");
            return;
        }

        var currentMappingCount = (int) genreMappingRepository.count();
        var newCount = newCategoryIds.size();

        if (currentMappingCount + newCount > properties.getGenreVectorSize()) {
            log.warn("Cannot add all {} new categories. Limit {} exceeded.", newCount, properties.getGenreVectorSize());
            newCategoryIds = newCategoryIds.subList(0, Math.max(0, properties.getGenreVectorSize() - currentMappingCount));
        }

        if (newCategoryIds.isEmpty()) return;

        var nextIndex = genreMappingRepository.findMaxVectorIndex().orElse(-1) + 1;

        var newMappings = new ArrayList<GenreMapping>();
        for (var categoryId : newCategoryIds) {
            var mapping = GenreMapping.builder()
                    .categoryId(categoryId)
                    .vectorIndex(nextIndex++)
                    .build();
            newMappings.add(mapping);
        }

        genreMappingRepository.saveAll(newMappings);
        log.info("Appended {} new category mappings.", newMappings.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildGenreMapping() {
        log.info("Rebuilding genre mapping from scratch...");
        genreMappingRepository.deleteAllInBatch();

        List<Integer> categoryIds = new ArrayList<>(categoryRepository.findAllIds());
        if (categoryIds.size() > properties.getGenreVectorSize()) {
            log.warn("Too many categories ({}). Limiting to {}.", categoryIds.size(), properties.getGenreVectorSize());
            categoryIds = categoryIds.subList(0, properties.getGenreVectorSize());
        }

        var newMappings = new ArrayList<GenreMapping>();
        for (int i = 0; i < categoryIds.size(); i++) {
            var mapping = GenreMapping.builder()
                    .categoryId(categoryIds.get(i))
                    .vectorIndex(i)
                    .build();
            newMappings.add(mapping);
        }

        genreMappingRepository.saveAll(newMappings);
        log.info("Genre mapping rebuilt with {} categories.", newMappings.size());
    }

}
