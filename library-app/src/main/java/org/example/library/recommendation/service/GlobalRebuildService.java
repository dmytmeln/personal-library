package org.example.library.recommendation.service;

import lombok.extern.slf4j.Slf4j;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.example.library.recommendation.domain.TfIdfVocabulary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GlobalRebuildService {


    private final BookRepository bookRepository;
    private final VocabularyService vocabularyService;
    private final VocabularyMetadataService metadataService;
    private final VectorizerService vectorizerService;
    private final GlobalRebuildService self;

    @Value("${recommendations.rebuild.batch-size:100}")
    private int batchSize;

    @Value("${recommendations.trigger.count:50}")
    private int countThreshold;

    public GlobalRebuildService(BookRepository bookRepository,
                                VocabularyService vocabularyService,
                                VocabularyMetadataService metadataService,
                                VectorizerService vectorizerService,
                                @Lazy GlobalRebuildService self) {
        this.bookRepository = bookRepository;
        this.vocabularyService = vocabularyService;
        this.metadataService = metadataService;
        this.vectorizerService = vectorizerService;
        this.self = self;
    }


    @Transactional
    public void executeFullRebuild() {
        var metadata = metadataService.getMetadata();
        var currentVersion = metadata.getCurrentVersion();

        long preliminaryCount = bookRepository.countWhereBookStatusNot(BookStatus.SYNCED);

        Map<String, TfIdfVocabulary> vocabulary = null;
        if (preliminaryCount >= countThreshold) {
            vocabulary = createNewVocabularyVersion(currentVersion + 1);
            currentVersion++;
        }

        processOutdatedBooksInBatches(currentVersion, Optional.ofNullable(vocabulary));

        vocabularyService.cleanUpOldVersions(currentVersion);
        log.info("Full rebuild completed. Current vocabulary version: {}", currentVersion);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBatch(List<Book> books, int version, Map<String, TfIdfVocabulary> vocabulary) {
        for (var book : books) {
            var vector = vectorizerService.calculateVector(book, vocabulary);
            book.setDescriptionVector(vector);
            book.setVectorVersion(version);
            book.setStatus(BookStatus.SYNCED);
        }

        bookRepository.saveAll(books);
    }

    private Map<String, TfIdfVocabulary> createNewVocabularyVersion(int newVersion) {
        log.info("Building vocabulary version {}", newVersion);

        var allDescriptions = bookRepository.findAllDescriptions("en");

        var newIdfMap = vocabularyService.calculateNewIdf(allDescriptions);
        var vocabulary = vocabularyService.saveVocabularyForVersion(newIdfMap, newVersion);

        metadataService.updateVersion();

        return vocabulary;
    }

    private void processOutdatedBooksInBatches(int targetVersion, Optional<Map<String, TfIdfVocabulary>> newVocabulary) {
        var totalToUpdate = bookRepository.countBooksWithOldVersion(targetVersion);
        log.info("Starting update of {} books to version {}", totalToUpdate, targetVersion);

        while (true) {
            var batch = bookRepository.findOutdatedBooks(targetVersion, PageRequest.of(0, batchSize));

            if (batch.isEmpty()) {
                break;
            }

            var vocabulary = newVocabulary
                    .orElseGet(() -> vocabularyService.getVocabularyForVersion(targetVersion));
            self.updateBatch(batch.getContent(), targetVersion, vocabulary);

            log.info("Processed batch: {} books", batch.getNumberOfElements());
        }
    }

}
