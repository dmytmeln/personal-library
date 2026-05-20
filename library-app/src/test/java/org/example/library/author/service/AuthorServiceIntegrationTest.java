package org.example.library.author.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.author.dto.AuthorSearchParams;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.repository.BookRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AuthorServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AuthorRepository repository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private AuthorService service;

    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void shouldReturnAuthorWhenGetById() {
        var expected = saveAuthor("John Doe", "USA");
        em.flush();
        em.clear();

        var result = service.getById(expected.getId());

        assertThat(result.fullName()).isEqualTo("John Doe");
        assertThat(result.country()).isEqualTo("USA");
    }

    @Test
    void shouldThrowNotFoundWhenGetByIdWithoutExistingAuthor() {
        assertThatThrownBy(() -> service.getById(-99999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.author.not_found");
    }

    @Test
    void shouldSearchAuthorsWithBooksCount() {
        var author = saveAuthor("Author 1", "UK");
        saveBook(author);
        saveBook(author);
        var otherAuthor = saveAuthor("Author 2", "USA");
        saveBook(otherAuthor);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        pagination.setSort(List.of("fullName;asc"));
        var searchParams = new AuthorSearchParams();
        searchParams.setName("Author 1");

        var result = service.search(pagination, searchParams);

        assertThat(result.getContent())
                .hasSize(2)
                .extracting("fullName")
                .contains("Author 1", "Author 2");
    }

    @Test
    void shouldSearchAuthorsByBooksCountRange() {
        var auth1 = saveAuthor("Auth 1", "Country");
        saveBook(auth1);
        var auth2 = saveAuthor("Auth 2", "Country");
        saveBook(auth2);
        saveBook(auth2);
        var auth3 = saveAuthor("Auth 3", "Country");
        saveBook(auth3);
        saveBook(auth3);
        saveBook(auth3);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        pagination.setSort(List.of("fullName;asc"));
        var searchParams = new AuthorSearchParams();
        searchParams.setBooksCountMin(2);
        searchParams.setBooksCountMax(2);

        var result = service.search(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Auth 2");
        assertThat(result.getContent().get(0).getBooksCount()).isEqualTo(2);
    }

    @Test
    void shouldFindAuthorWithTypo() {
        saveAuthor("John Doe", "USA");
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new AuthorSearchParams();
        searchParams.setName("John Doee");

        var result = service.search(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("John Doe");
    }

    @Test
    void shouldSearchAuthorsForUser() {
        var user = User.builder().email("test@example.com").fullName("Test User").password("pass").build();
        userRepository.save(user);
        var author = saveAuthor("User Author", "USA");
        var book1 = saveBook(author);
        var book2 = saveBook(author);
        libraryBookRepository.save(LibraryBook.builder().user(user).book(book1).title("Title 1").build());
        libraryBookRepository.save(LibraryBook.builder().user(user).book(book2).title("Title 2").build());
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        pagination.setSort(List.of("fullName;asc"));
        var searchParams = new AuthorSearchParams();

        var result = service.searchForUser(user.getId(), pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("User Author");
        assertThat(result.getContent().get(0).getBooksCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnAllCountries() {
        saveAuthor("Auth 1", "USA");
        saveAuthor("Auth 2", "UK");
        saveAuthor("Auth 3", "USA");
        em.flush();
        em.clear();

        var countries = service.getAllCountries();

        assertThat(countries).hasSize(2);
        assertThat(countries).extracting("country").containsExactlyInAnyOrder("USA", "UK");
    }

    @Test
    void shouldReturnCountriesForUser() {
        var user = User.builder().email("user@example.com").fullName("User").password("pass").build();
        userRepository.save(user);
        var auth1 = saveAuthor("Auth 1", "USA");
        var auth2 = saveAuthor("Auth 2", "UK");
        var book1 = saveBook(auth1);
        var book2 = saveBook(auth2);
        libraryBookRepository.save(LibraryBook.builder().user(user).book(book1).title("T1").build());
        libraryBookRepository.save(LibraryBook.builder().user(user).book(book2).title("T2").build());
        em.flush();
        em.clear();

        var countries = service.getCountriesForUser(user.getId());

        assertThat(countries).hasSize(2);
        assertThat(countries).extracting("country").containsExactlyInAnyOrder("USA", "UK");
    }

    private Author saveAuthor(String fullName, String country) {
        var author = Author.builder()
                .birthYear((short) 1900)
                .popularityCount(0)
                .build();
        var translation = AuthorTranslation.builder()
                .languageCode("en")
                .fullName(fullName)
                .country(country)
                .biography("Biography of " + fullName)
                .author(author)
                .build();
        author.setTranslations(Map.of("en", translation));
        return repository.save(author);
    }

    private Book saveBook(Author author) {
        var book = Book.builder()
                .authors(Set.of(author))
                .popularityCount(0)
                .build();
        return bookRepository.save(book);
    }

}
