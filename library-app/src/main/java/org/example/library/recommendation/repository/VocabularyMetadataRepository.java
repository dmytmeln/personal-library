package org.example.library.recommendation.repository;

import org.example.library.recommendation.domain.VocabularyMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabularyMetadataRepository extends JpaRepository<VocabularyMetadata, Integer> {
}
