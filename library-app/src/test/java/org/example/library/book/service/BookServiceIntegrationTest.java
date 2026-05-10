package org.example.library.book.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.dto.BookSearchParams;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.pagination.PaginationParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BookServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BookRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookService service;

    private Category defaultCategory;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @BeforeEach
    void init() {
        defaultCategory = saveCategory("Default Category");
    }


    @Test
    void shouldGetAllBooks() {
        saveBook("Book 1", "English");
        saveBook("Book 2", "English");
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new BookSearchParams();

        var result = service.getAll(pagination, searchParams);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void shouldFilterBooksByTitle() {
        saveBook("Spring in Action", "English");
        saveBook("Java Persistence with Hibernate", "English");
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new BookSearchParams();
        searchParams.setTitle("Spring");

        var result = service.getAll(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring in Action");
    }

    @Test
    void shouldFilterBooksByCategoryId() {
        var category = saveCategory("Fiction");
        var otherCategory = saveCategory("Sci-Fi");
        saveBook("Fictional Story", category);
        saveBook("Sci-Fi Adventure", otherCategory);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new BookSearchParams();
        searchParams.setCategoryId(category.getId());

        var result = service.getAll(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryId()).isEqualTo(category.getId());
    }

    @Test
    void shouldFilterBooksByAuthorId() {
        var author = saveAuthor("Author 1");
        var otherAuthor = saveAuthor("Author 2");
        saveBook("Book by A1", author);
        saveBook("Book by A2", otherAuthor);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new BookSearchParams();
        searchParams.setAuthorId(author.getId());

        var result = service.getAll(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthors().containsKey(author.getId())).isTrue();
    }

    @Test
    void shouldFilterBooksByPublishYearRange() {
        saveBook("Old Book", (short) 1990);
        saveBook("New Book", (short) 2020);
        saveBook("Mid Book", (short) 2005);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new BookSearchParams();
        searchParams.setPublishYearMin((short) 2000);
        searchParams.setPublishYearMax((short) 2010);

        var result = service.getAll(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Mid Book");
    }

    @Test
    void shouldFilterBooksByLanguages() {
        saveBook("English Book", "English");
        saveBook("French Book", "French");
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        var searchParams = new BookSearchParams();
        searchParams.setLanguages(List.of("French"));

        var result = service.getAll(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLanguage()).isEqualTo("French");
    }

    @Test
    void shouldReturnAllLanguages() {
        saveBook("B1", "English");
        saveBook("B2", "English");
        saveBook("B3", "French");
        em.flush();
        em.clear();

        var languages = service.getAllLanguages();

        assertThat(languages).hasSize(2);
        assertThat(languages).extracting("language").containsExactlyInAnyOrder("English", "French");
        assertThat(languages).filteredOn(l -> l.getLanguage().equals("English"))
                .extracting("count").containsExactly(2L);
    }


    private Category saveCategory(String name) {
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name(name)
                .description("Description of " + name)
                .build();
        var category = Category.builder()
                .popularityCount(0)
                .translations(Map.of("en", translation))
                .build();
        translation.setCategory(category);
        return categoryRepository.save(category);
    }

    private Author saveAuthor(String fullName) {
        var author = Author.builder()
                .birthYear((short) 1900)
                .popularityCount(0)
                .build();
        var translation = AuthorTranslation.builder()
                .languageCode("en")
                .fullName(fullName)
                .country("Country")
                .author(author)
                .build();
        author.setTranslations(Map.of("en", translation));
        return authorRepository.save(author);
    }

    private void saveBook(String title, String bookLanguage) {
        saveBook(title, bookLanguage, defaultCategory, (short) 2000, null);
    }

    private void saveBook(String title, Category category) {
        saveBook(title, "English", category, (short) 2000, null);
    }

    private void saveBook(String title, Author author) {
        saveBook(title, "English", defaultCategory, (short) 2000, author);
    }

    private void saveBook(String title, short publishYear) {
        saveBook(title, "English", defaultCategory, publishYear, null);
    }

    private void saveBook(String title, String bookLanguage, Category category, short publishYear, Author author) {
        var book = Book.builder()
                .category(category)
                .publishYear(publishYear)
                .popularityCount(0)
                .authors(author != null ? Set.of(author) : Set.of())
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage(bookLanguage)
                .description("Description of " + title)
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));
        repository.save(book);
    }

}
