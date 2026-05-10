package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.recommendation.domain.VocabularyMetadata;
import org.example.library.recommendation.repository.VocabularyMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyMetadataService {

    public static final int METADATA_ID = 1;


    private final VocabularyMetadataRepository metadataRepository;


    @Transactional
    public VocabularyMetadata getMetadata() {
        return metadataRepository.findById(METADATA_ID)
                .orElseThrow(() -> new IllegalStateException("Vocabulary metadata not found"));
    }

    @Transactional
    public void updateVersion() {
        var metadata = metadataRepository.findById(METADATA_ID)
                .orElseThrow(() -> new IllegalStateException("Vocabulary metadata not found"));
        metadata.setCurrentVersion(metadata.getCurrentVersion() + 1);
        metadata.setLastRebuildAt(LocalDateTime.now());

        metadataRepository.save(metadata);
        log.info("Incremented vocabulary version to {} and updated timestamp", metadata.getCurrentVersion());
    }

}
