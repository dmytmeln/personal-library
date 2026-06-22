package org.example.library.recommendation.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.recommendation.domain.UserProfileVector;
import org.example.library.recommendation.repository.UserProfileVectorRepository;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserProfileVectorRepository userProfileVectorRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser;
    private Category defaultCategory;


    @BeforeAll
    static void setUpAll() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            testUser = User.builder().email("test@example.com").fullName("Test User").password("pass").build();
            userRepository.save(testUser);

            var translation = CategoryTranslation.builder()
                    .languageCode("en")
                    .name("Fiction")
                    .description("Fiction category")
                    .build();
            defaultCategory = Category.builder()
                    .popularityCount(0)
                    .translations(Map.of("en", translation))
                    .build();
            translation.setCategory(defaultCategory);

            categoryRepository.save(defaultCategory);
        });
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            userProfileVectorRepository.deleteAll();
            libraryBookRepository.deleteAll();
            bookRepository.deleteAll();
            categoryRepository.deleteAll();
            userRepository.deleteAll();
        });
    }


    @Test
    void shouldReturnRecommendationsBasedOnVector() {
        transactionTemplate.executeWithoutResult(status -> {
            float[] userVector = new float[384];
            userVector[0] = 1.0f;
            saveUserProfileVector(testUser.getId(), userVector);

            float[] book3Vector = new float[384];
            book3Vector[0] = 0.9f;
            saveBook("Similar Book 2", book3Vector, (short) 2020);

            float[] book1Vector = new float[384];
            book1Vector[0] = 0.5f;
            saveBook("Similar Book 1", book1Vector, (short) 2020);

            float[] book2Vector = new float[384];
            book2Vector[1] = 1.0f;
            saveBook("Dissimilar Book", book2Vector, (short) 2020);
        });

        var recommendations = recommendationService.getRecommendations(testUser.getId(), 5);

        assertThat(recommendations).hasSize(3);
        assertThat(recommendations.get(0).getTitle()).isEqualTo("Similar Book 2");
        assertThat(recommendations.get(1).getTitle()).isEqualTo("Similar Book 1");
        assertThat(recommendations.get(2).getTitle()).isEqualTo("Dissimilar Book");
    }

    @Test
    void shouldReturnSimilarBooksExcludingTheTargetBook() {
        var targetBook = transactionTemplate.execute(status -> {
            float[] targetVector = new float[384];
            targetVector[0] = 1.0f;
            var book = saveBook("Target Book", targetVector, (short) 2020);
            float[] similarVector = new float[384];
            similarVector[0] = 0.9f;
            saveBook("Similar Book", similarVector, (short) 2020);
            return book;
        });

        var similarBooks = recommendationService.getSimilarBooks(targetBook.getId(), testUser.getId(), 5);

        assertThat(similarBooks).hasSize(1);
        assertThat(similarBooks.get(0).getTitle()).isEqualTo("Similar Book");
    }

    @Test
    void shouldReturnNewArrivalsForCurrentYear() {
        transactionTemplate.executeWithoutResult(status -> {
            saveBook("Old Book", new float[384], (short) 1999);
            saveBook("New Book", new float[384], (short) Year.now().getValue());
        });

        var newArrivals = recommendationService.getNewArrivals(testUser.getId(), 5);

        assertThat(newArrivals).hasSize(1);
        assertThat(newArrivals.get(0).getTitle()).isEqualTo("New Book");
    }

    @Test
    void shouldReturnPopularBooksRecently() {
        transactionTemplate.executeWithoutResult(status -> {
            var book1 = saveBook("Popular Book 1", new float[384], (short) 2020);
            var book2 = saveBook("Popular Book 2", new float[384], (short) 2020);
            var otherUser1 = User.builder().email("o1@example.com").fullName("O1").password("p").build();
            var otherUser2 = User.builder().email("o2@example.com").fullName("O2").password("p").build();
            userRepository.saveAll(List.of(otherUser1, otherUser2));
            libraryBookRepository.save(LibraryBook.builder().user(otherUser1).book(book1).addedAt(LocalDateTime.now()).build());
            libraryBookRepository.save(LibraryBook.builder().user(otherUser2).book(book1).addedAt(LocalDateTime.now()).build());
            libraryBookRepository.save(LibraryBook.builder().user(otherUser1).book(book2).addedAt(LocalDateTime.now()).build());
        });

        var popularBooks = recommendationService.getPopularBooks(testUser.getId(), 5);

        assertThat(popularBooks).hasSize(2);
        assertThat(popularBooks.get(0).getTitle()).isEqualTo("Popular Book 1"); // 2 adds
        assertThat(popularBooks.get(1).getTitle()).isEqualTo("Popular Book 2"); // 1 add
    }

    @Test
    void shouldReturnTrendingInFavoriteGenres() {
        transactionTemplate.executeWithoutResult(status -> {
            var otherCategory = Category.builder().popularityCount(0).build();
            var ct = CategoryTranslation.builder().languageCode("en").name("Sci-Fi").description("Sci-Fi").category(otherCategory).build();
            otherCategory.setTranslations(Map.of("en", ct));
            categoryRepository.save(otherCategory);
            var book1 = saveBook("Fiction Book 1", new float[384], (short) 2020, defaultCategory);
            saveBook("Fiction Book 2", new float[384], (short) 2020, defaultCategory);
            saveBook("Sci-Fi Book", new float[384], (short) 2020, otherCategory);
            libraryBookRepository.save(LibraryBook.builder().user(testUser).book(book1).addedAt(LocalDateTime.now()).build());
        });

        var trending = recommendationService.getTrendingInFavoriteGenres(testUser.getId(), 5);

        assertThat(trending).hasSize(1);
        assertThat(trending.get(0).getTitle()).isEqualTo("Fiction Book 2");
    }

    @Test
    void shouldReturnBooksBasedOnMoodQuery() {
        transactionTemplate.executeWithoutResult(status -> {
            float[] vector1 = new float[384]; vector1[0] = 0.9f;
            saveBook("Space Adventure", vector1, (short) 2024);

            float[] vector2 = new float[384]; vector2[0] = 0.8f;
            saveBook("Galactic Journey", vector2, (short) 2023);

            float[] vector3 = new float[384]; vector3[1] = 0.9f;
            saveBook("Historical Romance", vector3, (short) 2022);

            float[] vector4 = new float[384]; vector4[1] = 0.8f;
            saveBook("Medieval Love", vector4, (short) 2021);

            float[] vector5 = new float[384]; vector5[5] = 0.9f;
            saveBook("Cooking Basics", vector5, (short) 2020);
        });

        var results = recommendationService.searchByMood("space trip", testUser.getId(), 2);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getTitle()).containsAnyOf("Space Adventure", "Galactic Journey");
    }


    private Book saveBook(String title, float[] vector, short publishYear) {
        return saveBook(title, vector, publishYear, defaultCategory);
    }

    private Book saveBook(String title, float[] vector, short publishYear, Category category) {
        var book = Book.builder()
                .category(category)
                .publishYear(publishYear)
                .popularityCount(0)
                .embedding(vector)
                .status(BookStatus.NEW)
                .pages((short) 100)
                .coverImageUrl("url")
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage("English")
                .description("Description of " + title)
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));

        return bookRepository.save(book);
    }

    private void saveUserProfileVector(Integer userId, float[] vector) {
        var userProfileVector = UserProfileVector.builder()
                .userId(userId)
                .embedding(vector)
                .updatedAt(LocalDateTime.now())
                .build();

        userProfileVectorRepository.save(userProfileVector);
    }

}
