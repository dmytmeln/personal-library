package org.example.library.category.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.book.domain.Book;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.dto.CategorySearchParams;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.exception.NotFoundException;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.pagination.PaginationParams;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CategoryServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private CategoryService service;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }


    @Test
    void shouldReturnCategoryWhenGetById() {
        var expected = saveCategory("Test Category");
        em.flush();
        em.clear();

        var existingCategory = service.getById(expected.getId());

        assertThat(existingCategory.name()).isEqualTo(expected.getTranslations().get("en").getName());
        assertThat(existingCategory.description()).isEqualTo(expected.getTranslations().get("en").getDescription());
    }

    @Test
    void shouldThrowNotFoundWhenGetByIdWithoutExistingCategory() {
        assertThatThrownBy(() -> service.getById(-99999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.category.not_found");
    }

    @Test
    void shouldSearchCategoriesWithBooksCount() {
        var category = saveCategory("Fiction");
        saveBook(category);
        saveBook(category);
        var otherCategory = saveCategory("Science");
        saveBook(otherCategory);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        pagination.setSort(List.of("name;asc"));
        var searchParams = new CategorySearchParams();
        searchParams.setName("Fiction");

        var result = service.search(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Fiction");
        assertThat(result.getContent().get(0).getBooksCount()).isEqualTo(2);
    }

    @Test
    void shouldSearchCategoriesByBooksCountRange() {
        var cat1 = saveCategory("Cat 1");
        saveBook(cat1);
        var cat2 = saveCategory("Cat 2");
        saveBook(cat2);
        saveBook(cat2);
        var cat3 = saveCategory("Cat 3");
        saveBook(cat3);
        saveBook(cat3);
        saveBook(cat3);
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        pagination.setSort(List.of("name;asc"));
        var searchParams = new CategorySearchParams();
        searchParams.setBooksCountMin(2);
        searchParams.setBooksCountMax(2);

        var result = service.search(pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Cat 2");
        assertThat(result.getContent().get(0).getBooksCount()).isEqualTo(2);
    }

    @Test
    void shouldSearchCategoriesForUser() {
        var user = User.builder().email("test@example.com").fullName("Test User").password("pass").build();
        userRepository.save(user);
        var otherUser = User.builder().email("other@example.com").fullName("Other User").password("pass").build();
        userRepository.save(otherUser);
        var category = saveCategory("User Category");
        var book1 = saveBook(category);
        var book2 = saveBook(category);
        var book3 = saveBook(category);
        libraryBookRepository.save(LibraryBook.builder().user(user).book(book1).title("Title 1").build());
        libraryBookRepository.save(LibraryBook.builder().user(user).book(book2).title("Title 2").build());
        libraryBookRepository.save(LibraryBook.builder().user(otherUser).book(book3).title("Title 3").build());
        em.flush();
        em.clear();
        var pagination = new PaginationParams();
        pagination.setPage(0);
        pagination.setSize(10);
        pagination.setSort(List.of("name;asc"));
        var searchParams = new CategorySearchParams();

        var result = service.searchForUser(user.getId(), pagination, searchParams);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("User Category");
        assertThat(result.getContent().get(0).getBooksCount()).isEqualTo(2);
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
        return repository.save(category);
    }

    private Book saveBook(Category category) {
        var book = Book.builder()
                .category(category)
                .popularityCount(0)
                .build();
        return bookRepository.save(book);
    }

}