package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.recommendation.domain.TfIdfVocabulary;
import org.example.library.recommendation.domain.VocabularyMetadata;
import org.example.library.recommendation.repository.TfIdfVocabularyRepository;
import org.example.library.recommendation.repository.VocabularyMetadataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "recommendations.trigger.count=2",
        "recommendations.rebuild.batch-size=2"
})
class GlobalRebuildServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GlobalRebuildService globalRebuildService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VocabularyMetadataRepository metadataRepository;

    @Autowired
    private TfIdfVocabularyRepository vocabularyRepository;

    private Category defaultCategory;

    @BeforeAll
    static void setUpAll() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @BeforeEach
    void setUp() {
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name("IT")
                .description("IT Category")
                .build();
        defaultCategory = Category.builder()
                .popularityCount(0)
                .translations(Map.of("en", translation))
                .build();
        translation.setCategory(defaultCategory);

        categoryRepository.save(defaultCategory);

        if (metadataRepository.findById(VocabularyMetadataService.METADATA_ID).isEmpty()) {
            metadataRepository.save(VocabularyMetadata.builder()
                    .id(VocabularyMetadataService.METADATA_ID)
                    .currentVersion(1)
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        vocabularyRepository.deleteAll();
        metadataRepository.deleteAll();
    }


    @Test
    void shouldExecuteFullRebuildInBatchesAndCreateNewVocabulary() {
        saveBook("Java Book", "This book is about Java and Spring framework.");
        saveBook("Spring Book", "Spring is a popular framework for Java.");
        saveBook("Unrelated Book", "This is an unrelated programming book.");
        var initialMetadata = metadataRepository.findById(VocabularyMetadataService.METADATA_ID)
                .orElseThrow(() -> new AssertionError("Vocabulary metadata not found"));
        var initialVersion = initialMetadata.getCurrentVersion();

        globalRebuildService.executeFullRebuild();

        var updatedMetadata = metadataRepository.findById(VocabularyMetadataService.METADATA_ID)
                .orElseThrow(() -> new AssertionError("Vocabulary metadata not found"));
        assertThat(updatedMetadata.getCurrentVersion()).isEqualTo(initialVersion + 1);
        var newVocabulary = vocabularyRepository.findAllByVersion(initialVersion + 1);
        assertThat(newVocabulary).isNotEmpty();
        assertThat(newVocabulary).extracting(TfIdfVocabulary::getWord)
                .containsAnyOf("java", "spring", "framework", "book");
        var oldVocabulary = vocabularyRepository.findAllByVersion(initialVersion);
        assertThat(oldVocabulary).isEmpty();
        var books = bookRepository.findAll();
        assertThat(books).hasSize(3);
        assertThat(books).allSatisfy(book -> {
            assertThat(book.getStatus()).isEqualTo(BookStatus.SYNCED);
            assertThat(book.getVectorVersion()).isEqualTo(initialVersion + 1);
            assertThat(book.getDescriptionVector()).isNotEmpty();
        });
    }

    @Test
    void shouldUpdateOnlyOutdatedBooksWhenCountIsBelowThreshold() {
        var initialMetadata = metadataRepository.findById(VocabularyMetadataService.METADATA_ID)
                .orElseThrow(() -> new AssertionError("Vocabulary metadata not found"));
        var initialVersion = initialMetadata.getCurrentVersion();
        saveBook("Only Book", "A solitary book in the library.");


        globalRebuildService.executeFullRebuild();


        var updatedMetadata = metadataRepository.findById(VocabularyMetadataService.METADATA_ID)
                .orElseThrow(() -> new AssertionError("Vocabulary metadata not found"));
        assertThat(updatedMetadata.getCurrentVersion()).isEqualTo(initialVersion);
        var books = bookRepository.findAll();
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getStatus()).isEqualTo(BookStatus.SYNCED);
        assertThat(books.get(0).getVectorVersion()).isEqualTo(initialVersion);
    }


    private void saveBook(String title, String description) {
        var book = Book.builder()
                .category(defaultCategory)
                .publishYear((short) 2020)
                .popularityCount(0)
                .status(BookStatus.NEW)
                .vectorVersion(0)
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage("English")
                .description(description)
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));

        bookRepository.save(book);
    }

}