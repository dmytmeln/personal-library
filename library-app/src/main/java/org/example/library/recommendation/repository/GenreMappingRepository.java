package org.example.library.recommendation.repository;

import org.example.library.recommendation.domain.GenreMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface GenreMappingRepository extends JpaRepository<GenreMapping, Integer> {

    Optional<GenreMapping> findByCategoryId(Integer categoryId);

    @Query("SELECT g.categoryId FROM GenreMapping g")
    Set<Integer> findAllCategoryIds();

    @Query("SELECT MAX(g.vectorIndex) FROM GenreMapping g")
    Optional<Integer> findMaxVectorIndex();
}
