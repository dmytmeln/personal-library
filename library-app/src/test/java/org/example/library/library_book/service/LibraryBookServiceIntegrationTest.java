package org.example.library.library_book.service;

import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.collection.repository.CollectionRepository;
import org.example.library.collection_book.repository.CollectionBookRepository;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.dto.CreateLocalBookDto;
import org.example.library.library_book.dto.LibraryBookSearchCriteria;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.user.domain.Role;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryBookServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LibraryBookRepository repository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CollectionBookRepository collectionBookRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LibraryBookService service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User defaultUser;

    private Category defaultCategory;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @BeforeEach
    void init() {
        defaultUser = saveUser();
        defaultCategory = saveCategory();
    }

    @AfterEach
    void tearDown() {
        collectionBookRepository.deleteAll();
        collectionRepository.deleteAll();
        repository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        authorRepository.deleteAll();
        userRepository.deleteAll();
    }


    @Test
    void shouldGetAllByUserId() {
        var book = saveBook("Global Book");
        saveLibraryBook(book, defaultUser);
        var criteria = new LibraryBookSearchCriteria();
        var pagination = new PaginationParams();

        var result = service.getAllByUserId(defaultUser.getId(), criteria, pagination);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBook().getTitle()).isEqualTo("Global Book");
    }

    @Test
    void shouldCreateLocalBook() {
        var dto = CreateLocalBookDto.builder()
                .title("Local Book")
                .description("Local Description")
                .bookLanguage("uk")
                .status(LibraryBookStatus.READING)
                .build();

        service.createLocalBook(dto, defaultUser.getId());

        var libraryBooks = repository.findAllByUserIdWithBook(defaultUser.getId());
        assertThat(libraryBooks).hasSize(1);
        var saved = libraryBooks.get(0);
        assertThat(saved.getTitle()).isEqualTo("Local Book");
        assertThat(saved.getStatus()).isEqualTo(LibraryBookStatus.READING);
        assertThat(saved.getBook().getOwner().getId()).isEqualTo(defaultUser.getId());
    }

    @Test
    void shouldAddExistingBookToLibrary() {
        var book = saveBook("Existing Book");

        service.create(book.getId(), defaultUser.getId());

        var libraryBooks = repository.findAllByUserIdWithBook(defaultUser.getId());
        assertThat(libraryBooks).hasSize(1);
        assertThat(libraryBooks.get(0).getBook().getId()).isEqualTo(book.getId());
    }

    @Test
    void shouldRateLibraryBook() {
        var book = saveBook("Book to Rate");
        var libraryBook = saveLibraryBook(book, defaultUser);

        var result = service.rate(libraryBook.getId(), defaultUser.getId(), 5);

        assertThat(result.getRating()).isEqualTo((byte) 5);
        var updated = repository.findById(libraryBook.getId())
                .orElseThrow(() -> new AssertionError("Library book not found after rating"));
        assertThat(updated.getRating()).isEqualTo((byte) 5);
    }

    @Test
    void shouldUpdateStatus() {
        var book = saveBook("Book Status");
        var libraryBook = saveLibraryBook(book, defaultUser);
        libraryBook.setStatus(LibraryBookStatus.NO_TAG);
        repository.saveAndFlush(libraryBook);

        var result = service.updateStatus(libraryBook.getId(), defaultUser.getId(), LibraryBookStatus.READ);

        assertThat(result.getStatus()).isEqualTo(LibraryBookStatus.READ.name());
        var updated = repository.findById(libraryBook.getId())
                .orElseThrow(() -> new AssertionError("Library book not found after status update"));
        assertThat(updated.getStatus()).isEqualTo(LibraryBookStatus.READ);
    }

    @Test
    void shouldDeleteLibraryBook() {
        var book = saveBook("Book to Delete");
        var libraryBook = saveLibraryBook(book, defaultUser);

        service.delete(libraryBook.getId(), defaultUser.getId());

        assertThat(repository.existsById(libraryBook.getId())).isFalse();
    }

    @Test
    void shouldBulkAddBooks() {
        var book1 = saveBook("Book 1");
        var book2 = saveBook("Book 2");
        var book3 = saveBook("Book 3");
        saveLibraryBook(book1, defaultUser); // Already in library

        service.bulkAdd(List.of(book1.getId(), book2.getId(), book3.getId()), defaultUser.getId());

        var libraryBooks = repository.findAllByUserId(defaultUser.getId());
        assertThat(libraryBooks).hasSize(3);
        var bookIds = libraryBooks.stream().map(lb -> lb.getBook().getId()).toList();
        assertThat(bookIds).containsExactlyInAnyOrder(book1.getId(), book2.getId(), book3.getId());
    }

    @Test
    void shouldBulkUpdateStatus() {
        var lb1 = saveLibraryBook(saveBook("B1"), defaultUser);
        var lb2 = saveLibraryBook(saveBook("B2"), defaultUser);
        lb1.setStatus(LibraryBookStatus.TO_READ);
        lb2.setStatus(LibraryBookStatus.READING);
        repository.saveAllAndFlush(List.of(lb1, lb2));

        service.bulkUpdateStatus(List.of(lb1.getId(), lb2.getId()), defaultUser.getId(), LibraryBookStatus.READ);

        var updatedLb1 = repository.findById(lb1.getId()).orElseThrow();
        var updatedLb2 = repository.findById(lb2.getId()).orElseThrow();
        assertThat(updatedLb1.getStatus()).isEqualTo(LibraryBookStatus.READ);
        assertThat(updatedLb1.getFinishedAt()).isEqualTo(LocalDate.now());
        assertThat(updatedLb2.getStatus()).isEqualTo(LibraryBookStatus.READ);
        assertThat(updatedLb2.getFinishedAt()).isEqualTo(LocalDate.now());

        service.bulkUpdateStatus(List.of(lb1.getId()), defaultUser.getId(), LibraryBookStatus.READING);
        updatedLb1 = repository.findById(lb1.getId()).orElseThrow();
        assertThat(updatedLb1.getStatus()).isEqualTo(LibraryBookStatus.READING);
        assertThat(updatedLb1.getFinishedAt()).isNull();
    }

    @Test
    void shouldBulkDeleteBooks() {
        var lb1 = saveLibraryBook(saveBook("B1"), defaultUser);
        var lb2 = saveLibraryBook(saveBook("B2"), defaultUser);

        service.bulkDelete(List.of(lb1.getId(), lb2.getId()), defaultUser.getId());

        assertThat(repository.existsById(lb1.getId())).isFalse();
        assertThat(repository.existsById(lb2.getId())).isFalse();
    }

    @Test
    void shouldSearchByMoodWithoutStatusFilter() {
        transactionTemplate.executeWithoutResult(status -> {
            float[] v1 = new float[384];
            v1[0] = 0.9f;
            saveLibraryBook(saveBook("Space Adventure", v1), defaultUser, LibraryBookStatus.READ);

            float[] v2 = new float[384];
            v2[0] = 0.8f;
            saveLibraryBook(saveBook("Galactic Journey", v2), defaultUser, LibraryBookStatus.TO_READ);

            float[] v3 = new float[384];
            v3[1] = 0.9f;
            saveLibraryBook(saveBook("Historical Romance", v3), defaultUser, LibraryBookStatus.TO_READ);

            float[] v4 = new float[384];
            v4[1] = 0.8f;
            saveLibraryBook(saveBook("Medieval Love", v4), defaultUser, LibraryBookStatus.READING);

            float[] v5 = new float[384];
            v5[5] = 0.9f;
            saveLibraryBook(saveBook("Cooking Basics", v5), defaultUser, LibraryBookStatus.TO_READ);
        });

        var results = service.searchByMood("space trip", null, defaultUser.getId(), 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getBook().getTitle()).containsAnyOf("Space Adventure", "Galactic Journey");
        assertThat(results.get(1).getBook().getTitle()).containsAnyOf("Space Adventure", "Galactic Journey");
    }

    @Test
    void shouldSearchByMoodWithStatusFilter() {
        transactionTemplate.executeWithoutResult(status -> {
            float[] v1 = new float[384];
            v1[0] = 0.9f;
            saveLibraryBook(saveBook("Space Adventure", v1), defaultUser, LibraryBookStatus.READ);

            float[] v2 = new float[384];
            v2[0] = 0.8f;
            saveLibraryBook(saveBook("Galactic Journey", v2), defaultUser, LibraryBookStatus.TO_READ);

            float[] v3 = new float[384];
            v3[1] = 0.9f;
            saveLibraryBook(saveBook("Historical Romance", v3), defaultUser, LibraryBookStatus.TO_READ);

            float[] v4 = new float[384];
            v4[1] = 0.8f;
            saveLibraryBook(saveBook("Medieval Love", v4), defaultUser, LibraryBookStatus.READING);

            float[] v5 = new float[384];
            v5[5] = 0.9f;
            saveLibraryBook(saveBook("Cooking Basics", v5), defaultUser, LibraryBookStatus.TO_READ);
        });

        var results = service.searchByMood("space trip", LibraryBookStatus.TO_READ, defaultUser.getId(), 2);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getBook().getTitle()).isEqualTo("Galactic Journey");
        assertThat(results.stream().anyMatch(r -> r.getBook().getTitle().equals("Space Adventure"))).isFalse();
    }


    private User saveUser() {
        var user = User.builder()
                .email("test@example.com")
                .fullName("Test User")
                .password("password")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private Category saveCategory() {
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name("Default Category")
                .description("Description")
                .build();
        var category = Category.builder()
                .popularityCount(0)
                .translations(Map.of("en", translation))
                .build();
        translation.setCategory(category);
        return categoryRepository.save(category);
    }

    private Book saveBook(String title) {
        return saveBook(title, null);
    }

    private Book saveBook(String title, float[] embedding) {
        var book = Book.builder()
                .category(defaultCategory)
                .owner(null)
                .status(BookStatus.SYNCED)
                .embedding(embedding)
                .popularityCount(0)
                .authors(Set.of())
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage("en")
                .description("Description")
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));

        return bookRepository.save(book);
    }

    private LibraryBook saveLibraryBook(Book book, User user) {
        return saveLibraryBook(book, user, LibraryBookStatus.TO_READ);
    }

    private LibraryBook saveLibraryBook(Book book, User user, LibraryBookStatus status) {
        var libraryBook = LibraryBook.of(book, user);
        libraryBook.setStatus(status);

        return repository.save(libraryBook);
    }

}
