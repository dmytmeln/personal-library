package org.example.library.book.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.exception.NotFoundException;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class BookDetailsServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BookDetailsService service;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }


    @Test
    void shouldReturnDetailsWithLibraryBookWhenInUserLibrary() {
        var user = saveUser("test@example.com");
        var author = saveAuthor();
        var category = saveCategory();
        var book = saveBook("Book Title", author, category);
        saveLibraryBook(user, book, "User Title");
        em.flush();
        em.clear();

        var details = service.getDetails(book.getId(), user.getId());

        assertThat(details.libraryBook()).isNotNull();
        assertThat(details.libraryBook().getBook().getTitle()).isEqualTo("User Title");
        assertThat(details.book()).isNull();
        assertThat(details.averageRating()).isEqualTo(0.0);
        assertThat(details.ratingsNumber()).isEqualTo(0L);
    }

    @Test
    void shouldReturnDetailsWithBookWhenNotInUserLibrary() {
        var user = saveUser("other@example.com");
        var author = saveAuthor();
        var category = saveCategory();
        var book = saveBook("Book Title", author, category);
        em.flush();
        em.clear();

        var details = service.getDetails(book.getId(), user.getId());

        assertThat(details.book()).isNotNull();
        assertThat(details.book().getTitle()).isEqualTo("Book Title");
        assertThat(details.libraryBook()).isNull();
    }

    @Test
    void shouldReturnDetailsWithAverageRating() {
        var user1 = saveUser("user1@example.com");
        var user2 = saveUser("user2@example.com");
        var author = saveAuthor();
        var category = saveCategory();
        var book = saveBook("Rated Book", author, category);
        var lb1 = saveLibraryBook(user1, book, "Title 1");
        lb1.setRating((byte) 5);
        libraryBookRepository.save(lb1);
        var lb2 = saveLibraryBook(user2, book, "Title 2");
        lb2.setRating((byte) 3);
        libraryBookRepository.save(lb2);
        em.flush();
        em.clear();

        var details = service.getDetails(book.getId(), user1.getId());

        assertThat(details.averageRating()).isEqualTo(4.0);
        assertThat(details.ratingsNumber()).isEqualTo(2L);
    }

    @Test
    void shouldThrowNotFoundWhenBookDoesNotExist() {
        var user = saveUser("test@example.com");

        assertThatThrownBy(() -> service.getDetails(-999, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.book.not_found");
    }


    private User saveUser(String email) {
        var user = User.builder()
                .email(email)
                .fullName("Test User")
                .password("password")
                .build();

        return userRepository.save(user);
    }

    private Author saveAuthor() {
        var author = Author.builder()
                .birthYear((short) 1900)
                .popularityCount(0)
                .build();
        var translation = AuthorTranslation.builder()
                .languageCode("en")
                .fullName("Author Name")
                .country("Country")
                .author(author)
                .build();
        author.setTranslations(Map.of("en", translation));

        return authorRepository.save(author);
    }

    private Category saveCategory() {
        var category = Category.builder()
                .popularityCount(0)
                .build();
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name("Category Name")
                .category(category)
                .build();
        category.setTranslations(Map.of("en", translation));

        return categoryRepository.save(category);
    }

    private Book saveBook(String title, Author author, Category category) {
        var book = Book.builder()
                .popularityCount(0)
                .authors(author != null ? Set.of(author) : Set.of())
                .category(category)
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage("en")
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));

        return bookRepository.save(book);
    }

    private LibraryBook saveLibraryBook(User user, Book book, String title) {
        var libraryBook = LibraryBook.builder()
                .user(user)
                .book(book)
                .title(title)
                .build();

        return libraryBookRepository.save(libraryBook);
    }

}
