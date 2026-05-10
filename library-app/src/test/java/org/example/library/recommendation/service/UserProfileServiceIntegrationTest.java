package org.example.library.recommendation.service;

import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.recommendation.domain.VocabularyMetadata;
import org.example.library.recommendation.event.UserProfileUpdatedEvent;
import org.example.library.recommendation.repository.UserProfileVectorRepository;
import org.example.library.recommendation.repository.VocabularyMetadataRepository;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class UserProfileServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileVectorRepository userProfileVectorRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VocabularyMetadataRepository metadataRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
        testUser = User.builder().email("test@example.com").fullName("Test User").password("pass").build();
        userRepository.save(testUser);

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
        libraryBookRepository.deleteAll();
        userProfileVectorRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        metadataRepository.deleteAll();
    }


    @Test
    void shouldCalculateAndSaveUserProfileVector() {
        var book = saveBook("Java Book", new float[1100]);
        book.getDescriptionVector()[0] = 1.0f;
        bookRepository.save(book);
        var lb = LibraryBook.builder()
                .user(testUser)
                .book(book)
                .status(LibraryBookStatus.FAVORITE)
                .addedAt(LocalDateTime.now())
                .build();
        libraryBookRepository.save(lb);

        var vector = userProfileService.calculateUserProfileVector(testUser.getId());

        assertThat(vector).isNotNull();
        assertThat(vector).hasSize(1100);
        var savedVector = userProfileVectorRepository.findById(testUser.getId());
        assertThat(savedVector).isPresent();
        assertThat(savedVector.get().getVector()).isEqualTo(vector);
        assertThat(savedVector.get().getVersion()).isEqualTo(1);
    }

    @Test
    void shouldAsynchronouslyRebuildVectorOnEventAfterCommit() {
        var book = saveBook("Java Book", new float[1100]);
        book.getDescriptionVector()[0] = 1.0f;
        bookRepository.save(book);

        transactionTemplate.executeWithoutResult(status -> {
            var lb = LibraryBook.builder()
                    .user(testUser)
                    .book(book)
                    .status(LibraryBookStatus.FAVORITE)
                    .addedAt(LocalDateTime.now())
                    .build();
            libraryBookRepository.save(lb);

            eventPublisher.publishEvent(new UserProfileUpdatedEvent(testUser.getId()));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var savedVector = userProfileVectorRepository.findById(testUser.getId());
            assertThat(savedVector).isPresent();
            assertThat(savedVector.get().getVector()[0]).isGreaterThan(0.0f);
            assertThat(savedVector.get().getVersion()).isEqualTo(1);
        });
    }
    @Test
    void shouldDeleteVectorOnEventWhenLibraryIsEmpty() {
        var book = saveBook("Java Book", new float[1100]);
        book.getDescriptionVector()[0] = 1.0f;
        bookRepository.save(book);
        libraryBookRepository.save(LibraryBook.builder().user(testUser).book(book).status(LibraryBookStatus.READING).addedAt(LocalDateTime.now()).build());
        userProfileService.calculateUserProfileVector(testUser.getId());
        assertThat(userProfileVectorRepository.findById(testUser.getId())).isPresent();

        transactionTemplate.executeWithoutResult(status -> {
            libraryBookRepository.deleteAll();
            eventPublisher.publishEvent(new UserProfileUpdatedEvent(testUser.getId()));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var savedVector = userProfileVectorRepository.findById(testUser.getId());
            assertThat(savedVector).isEmpty();
        });
    }


    private Book saveBook(String title, float[] descriptionVector) {
        var book = Book.builder()
                .category(defaultCategory)
                .publishYear((short) 2020)
                .popularityCount(0)
                .status(BookStatus.SYNCED)
                .vectorVersion(1)
                .descriptionVector(descriptionVector)
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage("English")
                .description("Description")
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));

        return bookRepository.save(book);
    }

}