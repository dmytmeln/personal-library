package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.recommendation.config.RecommendationProperties;
import org.example.library.recommendation.domain.TfIdfVocabulary;
import org.example.library.recommendation.repository.GenreMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smile.nlp.tokenizer.SimpleTokenizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorizerService {

    private static final String EN_LANGUAGE_CODE = "en";


    private final GenreMappingRepository genreMappingRepository;
    private final SimpleTokenizer simpleTokenizer;
    private final RecommendationProperties properties;


    @Transactional
    public float[] calculateVector(Book book, Map<String, TfIdfVocabulary> vocabulary) {
        var vector = new float[properties.getTotalVectorSize()];

        var description = getEnglishDescription(book);
        if (description == null || description.isBlank()) {
            return null;
        }

        var words = simpleTokenizer.split(description.toLowerCase());

        var counts = new HashMap<String, Integer>();
        for (var word : words) {
            counts.put(word, counts.getOrDefault(word, 0) + 1);
        }

        for (var entry : counts.entrySet()) {
            var word = entry.getKey();
            if (vocabulary.containsKey(word)) {
                var vocab = vocabulary.get(word);

                var tf = (double) entry.getValue() / words.length;
                var tfIdf = tf * vocab.getIdfScore();

                vector[vocab.getVectorIndex()] = (float) tfIdf;
            }
        }

        var category = book.getCategory();
        if (category != null) {
            var genreMapping = genreMappingRepository.findByCategoryId(category.getId());
            if (genreMapping.isPresent()) {
                int index = properties.getTextVectorSize() + genreMapping.get().getVectorIndex();
                vector[index] = properties.getGenreCoefficient();
            }
        }

        return vector;
    }

    private String getEnglishDescription(Book book) {
        var translations = book.getTranslations();
        if (translations == null) {
            return null;
        }

        var englishTranslation = translations.get(EN_LANGUAGE_CODE);
        return Optional.ofNullable(englishTranslation)
                .map(BookTranslation::getDescription)
                .orElse(null);
    }

}
