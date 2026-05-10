package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.book.repository.BookRepository;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.TfIdfVocabulary;
import org.example.library.recommendation.repository.TfIdfVocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smile.nlp.dictionary.EnglishStopWords;
import smile.nlp.tokenizer.SimpleTokenizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyService {

    private final TfIdfVocabularyRepository vocabularyRepository;
    private final BookRepository bookRepository;
    private final SimpleTokenizer simpleTokenizer;
    private final RecommendationProperties properties;


    public Map<String, Double> calculateNewIdf(List<String> descriptions) {
        var docFrequency = new HashMap<String, Integer>();
        var totalDocs = descriptions.size();

        for (var desc : descriptions) {
            if (desc == null || desc.isBlank()) {
                continue;
            }

            var words = simpleTokenizer.split(desc.toLowerCase());
            var uniqueWords = Arrays.stream(words)
                    .filter(w -> w.length() > 2)
                    .filter(w -> !EnglishStopWords.DEFAULT.contains(w))
                    .collect(Collectors.toSet());

            for (var word : uniqueWords) {
                docFrequency.put(word, docFrequency.getOrDefault(word, 0) + 1);
            }
        }

        var idfMap = new HashMap<String, Double>();
        for (var entry : docFrequency.entrySet()) {
            var idf = Math.log10((double) totalDocs / entry.getValue());
            idfMap.put(entry.getKey(), idf);
        }

        return idfMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(properties.getTextVectorSize())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Transactional
    public Map<String, TfIdfVocabulary> saveVocabularyForVersion(Map<String, Double> idfMap, int version) {
        var entries = new HashMap<String, TfIdfVocabulary>();
        var index = 0;

        for (var entry : idfMap.entrySet()) {
            var vocab = TfIdfVocabulary.builder()
                    .word(entry.getKey())
                    .idfScore(entry.getValue())
                    .vectorIndex(index++)
                    .version(version)
                    .build();
            entries.put(entry.getKey(), vocab);
        }

        var saved = vocabularyRepository.saveAll(entries.values()).size();
        if (saved != entries.size()) {
            log.warn("Expected to save {} vocabulary entries, but actually saved {}", entries.size(), saved);
            throw new IllegalStateException("Vocabulary save mismatch: expected " + entries.size() + ", but saved " + saved);
        }

        log.info("Successfully saved {} vocabulary entries for version {}", saved, version);
        return entries;
    }

    public Map<String, TfIdfVocabulary> getVocabularyForVersion(int version) {
        return vocabularyRepository.findAllByVersion(version).stream()
                .collect(Collectors.toMap(TfIdfVocabulary::getWord, v -> v));
    }

    public void cleanUpOldVersions(int currentVersion) {
        var outdatedCount = bookRepository.countBooksWithOldVersion(currentVersion);

        if (outdatedCount == 0) {
            log.info("All books synchronized with version {}. Deleting old vocabularies...", currentVersion);
            vocabularyRepository.deleteByVersionLessThan(currentVersion);
        } else {
            log.warn("Cleanup cancelled: found {} books that still need old vocabularies.", outdatedCount);
        }
    }

}
