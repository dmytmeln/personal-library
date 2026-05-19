package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "recommendations.trigger.count=2",
        "recommendations.rebuild.batch-size=1"
})
class GlobalRebuildServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GlobalRebuildService globalRebuildService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    }

    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void shouldUpdateBooksMissingEmbeddings() {
        saveBook("Only Book", "Description");

        globalRebuildService.executeFullRebuild();

        var books = bookRepository.findAll();
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getStatus()).isEqualTo(BookStatus.SYNCED);
        assertThat(books.get(0).getEmbedding()).isNotNull();
        assertThat(books.get(0).getEmbedding()).hasSize(384);
    }

    @Test
    void shouldNotUpdateBooksThatAlreadyHaveEmbeddings() {
        float[] existingEmbedding = new float[384];
        existingEmbedding[0] = 0.5f;
        saveBookWithEmbedding("Existing Book", "Description", existingEmbedding);

        globalRebuildService.executeFullRebuild();

        var books = bookRepository.findAll();
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getEmbedding()).isEqualTo(existingEmbedding);
    }

    @Test
    void shouldProcessMultipleBatchesCorrectly() {
        saveBook("Book 1", "Desc 1");
        saveBook("Book 2", "Desc 2");
        saveBook("Book 3", "Desc 3");

        globalRebuildService.executeFullRebuild();

        var books = bookRepository.findAll();
        assertThat(books).hasSize(3);
        assertThat(books).allMatch(b -> b.getEmbedding() != null);
        assertThat(books).allMatch(b -> b.getStatus() == BookStatus.SYNCED);
    }

    @Test
    void shouldUpdateOnlyBooksWithoutEmbeddingsInMixedScenario() {
        float[] existingEmbedding = new float[384];
        existingEmbedding[0] = 0.7f;
        saveBookWithEmbedding("Has Embedding", "Desc", existingEmbedding);
        saveBook("No Embedding", "Desc");

        globalRebuildService.executeFullRebuild();

        transactionTemplate.executeWithoutResult(status -> {
            var allBooks = bookRepository.findAll();
            var bookWithEmbedding = allBooks.stream()
                    .filter(b -> b.getTranslations().get("en").getTitle().equals("Has Embedding"))
                    .findFirst().orElseThrow();
            var bookWithoutEmbedding = allBooks.stream()
                    .filter(b -> b.getTranslations().get("en").getTitle().equals("No Embedding"))
                    .findFirst().orElseThrow();
            assertThat(bookWithEmbedding.getEmbedding()).isEqualTo(existingEmbedding);
            assertThat(bookWithoutEmbedding.getEmbedding()).isNotNull();
            assertThat(bookWithoutEmbedding.getStatus()).isEqualTo(BookStatus.SYNCED);
        });
    }

    private void saveBook(String title, String description) {
        saveBookWithEmbedding(title, description, null);
    }

    private void saveBookWithEmbedding(String title, String description, float[] embedding) {
        var book = Book.builder()
                .category(defaultCategory)
                .publishYear((short) 2020)
                .pages((short) 100)
                .coverImageUrl("url")
                .popularityCount(0)
                .status(embedding == null ? BookStatus.NEW : BookStatus.SYNCED)
                .embedding(embedding)
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
