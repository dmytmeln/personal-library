package org.example.library.library_book.service;

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
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.dto.CreateLocalBookDto;
import org.example.library.library_book.dto.LibraryBookSearchCriteria;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.pagination.PaginationParams;
import org.example.library.user.domain.Role;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LibraryBookServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private LibraryBookRepository repository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LibraryBookService service;

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


    @Test
    void shouldGetAllByUserId() {
        var book = saveBook("Global Book");
        saveLibraryBook(book, defaultUser);
        em.flush();
        em.clear();
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

        var libraryBooks = repository.findAllByUserId(defaultUser.getId());
        assertThat(libraryBooks).hasSize(1);
        var saved = libraryBooks.get(0);
        assertThat(saved.getTitle()).isEqualTo("Local Book");
        assertThat(saved.getStatus()).isEqualTo(LibraryBookStatus.READING);
        assertThat(saved.getBook().getOwner().getId()).isEqualTo(defaultUser.getId());
    }

    @Test
    void shouldAddExistingBookToLibrary() {
        var book = saveBook("Existing Book");
        em.flush();
        em.clear();

        service.create(book.getId(), defaultUser.getId());

        var libraryBooks = repository.findAllByUserId(defaultUser.getId());
        assertThat(libraryBooks).hasSize(1);
        assertThat(libraryBooks.get(0).getBook().getId()).isEqualTo(book.getId());
    }

    @Test
    void shouldRateLibraryBook() {
        var book = saveBook("Book to Rate");
        var libraryBook = saveLibraryBook(book, defaultUser);
        em.flush();
        em.clear();

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
        em.clear();

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
        em.flush();
        em.clear();

        service.delete(libraryBook.getId(), defaultUser.getId());

        assertThat(repository.existsById(libraryBook.getId())).isFalse();
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
        var book = Book.builder()
                .category(defaultCategory)
                .owner(null)
                .status(BookStatus.NEW)
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
        var libraryBook = LibraryBook.of(book, user);
        libraryBook.setStatus(LibraryBookStatus.TO_READ);
        return repository.save(libraryBook);
    }

}
