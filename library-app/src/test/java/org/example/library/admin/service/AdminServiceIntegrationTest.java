package org.example.library.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.admin.dto.AdminAuthorDto;
import org.example.library.admin.dto.AdminBookDto;
import org.example.library.admin.dto.AdminCategoryDto;
import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.common.exception.BadRequestException;
import org.example.library.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AdminServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AdminService service;


    @Test
    void shouldReturnBookWhenGetById() {
        var category = saveCategory("Category");
        var author = saveAuthor("Author");
        var book = saveBook("Book Title", category, Set.of(author));
        em.flush();
        em.clear();

        var result = service.getBook(book.getId());

        assertThat(result.getId()).isEqualTo(book.getId());
        assertThat(result.getCategoryId()).isEqualTo(category.getId());
        assertThat(result.getAuthorIds()).containsExactly(author.getId());
        assertThat(result.getTranslations().get("en").getTitle()).isEqualTo("Book Title");
    }

    @Test
    void shouldThrowNotFoundWhenGetBookByIdWithoutExistingBook() {
        assertThatThrownBy(() -> service.getBook(-1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.book.not_found");
    }

    @Test
    void shouldCreateBook() {
        var category = saveCategory("Category");
        var author = saveAuthor("Author");
        var dto = AdminBookDto.builder()
                .categoryId(category.getId())
                .authorIds(List.of(author.getId()))
                .publishYear((short) 2020)
                .pages((short) 300)
                .translations(Map.of("en", AdminBookDto.AdminBookTranslationDto.builder()
                        .title("New Book")
                        .bookLanguage("English")
                        .description("Desc")
                        .build()))
                .build();

        service.createBook(dto);
        em.flush();
        em.clear();

        var books = bookRepository.findAll();
        assertThat(books).hasSize(1);
        var book = books.get(0);
        assertThat(book.getCategory().getId()).isEqualTo(category.getId());
        assertThat(book.getAuthors()).hasSize(1);
        assertThat(book.getTranslations().get("en").getTitle()).isEqualTo("New Book");
        assertThat(book.getStatus()).isEqualTo(BookStatus.PRELIMINARY);
    }

    @Test
    void shouldUpdateBook() {
        var book = saveBook("Old Title", null, Set.of());
        var newCategory = saveCategory("New Category");
        var dto = AdminBookDto.builder()
                .categoryId(newCategory.getId())
                .publishYear((short) 2021)
                .pages(book.getPages())
                .translations(Map.of("en", AdminBookDto.AdminBookTranslationDto.builder()
                        .title("New Title")
                        .bookLanguage("English")
                        .build()))
                .build();

        service.updateBook(book.getId(), dto);
        em.flush();
        em.clear();

        var updatedBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> new AssertionError("Book not found after update"));
        assertThat(updatedBook.getCategory().getId()).isEqualTo(newCategory.getId());
        assertThat(updatedBook.getTranslations().get("en").getTitle()).isEqualTo("New Title");
        assertThat(updatedBook.getPublishYear()).isEqualTo((short) 2021);
    }

    @Test
    void shouldDeleteBook() {
        var book = saveBook("Title", null, Set.of());
        em.flush();
        em.clear();

        service.deleteBook(book.getId());
        em.flush();
        em.clear();

        assertThat(bookRepository.existsById(book.getId())).isFalse();
    }

    @Test
    void shouldDeleteBooksBulk() {
        var b1 = saveBook("B1", null, Set.of());
        var b2 = saveBook("B2", null, Set.of());
        em.flush();
        em.clear();

        service.deleteBooks(List.of(b1.getId(), b2.getId()));
        em.flush();
        em.clear();

        assertThat(bookRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnAuthorWhenGetById() {
        var author = saveAuthor("Author Name");
        em.flush();
        em.clear();

        var result = service.getAuthor(author.getId());

        assertThat(result.getId()).isEqualTo(author.getId());
        assertThat(result.getTranslations().get("en").getFullName()).isEqualTo("Author Name");
    }

    @Test
    void shouldThrowNotFoundWhenGetAuthorByIdWithoutExistingAuthor() {
        assertThatThrownBy(() -> service.getAuthor(-1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.author.not_found");
    }

    @Test
    void shouldCreateAuthor() {
        var dto = AdminAuthorDto.builder()
                .birthYear((short) 1950)
                .translations(Map.of("en", AdminAuthorDto.AdminAuthorTranslationDto.builder()
                        .fullName("New Author")
                        .country("USA")
                        .biography("Bio")
                        .build()))
                .build();

        service.createAuthor(dto);
        em.flush();
        em.clear();

        var authors = authorRepository.findAll();
        assertThat(authors).hasSize(1);
        var author = authors.get(0);
        assertThat(author.getTranslations().get("en").getFullName()).isEqualTo("New Author");
        assertThat(author.getBirthYear()).isEqualTo((short) 1950);
    }

    @Test
    void shouldUpdateAuthor() {
        var author = saveAuthor("Old Name");
        var dto = AdminAuthorDto.builder()
                .birthYear(author.getBirthYear())
                .deathYear((short) 2000)
                .translations(Map.of("en", AdminAuthorDto.AdminAuthorTranslationDto.builder()
                        .fullName("New Name")
                        .country("USA")
                        .build()))
                .build();
        em.flush();
        em.clear();

        service.updateAuthor(author.getId(), dto);
        em.flush();
        em.clear();

        var updatedAuthor = authorRepository.findById(author.getId())
                .orElseThrow(() -> new AssertionError("Author not found after update"));
        assertThat(updatedAuthor.getTranslations().get("en").getFullName()).isEqualTo("New Name");
        assertThat(updatedAuthor.getDeathYear()).isEqualTo((short) 2000);
    }

    @Test
    void shouldDeleteAuthor() {
        var author = saveAuthor("Author");
        em.flush();
        em.clear();

        service.deleteAuthor(author.getId());
        em.flush();
        em.clear();

        assertThat(authorRepository.existsById(author.getId())).isFalse();
    }

    @Test
    void shouldThrowBadRequestWhenDeleteAuthorWithBooks() {
        var author = saveAuthor("Author");
        saveBook("Book", null, Set.of(author));
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.deleteAuthor(author.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.author.has_books");
    }

    @Test
    void shouldDeleteAuthorsBulk() {
        var a1 = saveAuthor("A1");
        var a2 = saveAuthor("A2");
        em.flush();
        em.clear();

        service.deleteAuthors(List.of(a1.getId(), a2.getId()));
        em.flush();
        em.clear();

        assertThat(authorRepository.findAll()).isEmpty();
    }

    @Test
    void shouldThrowBadRequestWhenDeleteAuthorsBulkWithBooks() {
        var a1 = saveAuthor("A1");
        var a2 = saveAuthor("A2");
        saveBook("Book", null, Set.of(a1));
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.deleteAuthors(List.of(a1.getId(), a2.getId())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.author.has_books");
    }

    @Test
    void shouldReturnCategoryWhenGetById() {
        var category = saveCategory("Category Name");
        em.flush();
        em.clear();

        var result = service.getCategory(category.getId());

        assertThat(result.getId()).isEqualTo(category.getId());
        assertThat(result.getTranslations().get("en").getName()).isEqualTo("Category Name");
    }

    @Test
    void shouldThrowNotFoundWhenGetCategoryByIdWithoutExistingCategory() {
        assertThatThrownBy(() -> service.getCategory(-1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.category.not_found");
    }

    @Test
    void shouldCreateCategory() {
        var dto = AdminCategoryDto.builder()
                .translations(Map.of("en", AdminCategoryDto.AdminCategoryTranslationDto.builder()
                        .name("New Category")
                        .description("Desc")
                        .build()))
                .build();

        service.createCategory(dto);
        em.flush();
        em.clear();

        var categories = categoryRepository.findAll();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getTranslations().get("en").getName()).isEqualTo("New Category");
    }

    @Test
    void shouldUpdateCategory() {
        var category = saveCategory("Old Name");
        var dto = AdminCategoryDto.builder()
                .translations(Map.of("en", AdminCategoryDto.AdminCategoryTranslationDto.builder()
                        .name("New Name")
                        .build()))
                .build();
        em.flush();
        em.clear();

        service.updateCategory(category.getId(), dto);
        em.flush();
        em.clear();

        var updatedCategory = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new AssertionError("Category not found after update"));
        assertThat(updatedCategory.getTranslations().get("en").getName()).isEqualTo("New Name");
    }

    @Test
    void shouldDeleteCategory() {
        var category = saveCategory("Category");
        em.flush();
        em.clear();

        service.deleteCategory(category.getId());
        em.flush();
        em.clear();

        assertThat(categoryRepository.existsById(category.getId())).isFalse();
    }

    @Test
    void shouldThrowBadRequestWhenDeleteCategoryWithBooks() {
        var category = saveCategory("Category");
        saveBook("Book", category, Set.of());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.deleteCategory(category.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.category.has_books");
    }

    @Test
    void shouldDeleteCategoriesBulk() {
        var c1 = saveCategory("C1");
        var c2 = saveCategory("C2");
        em.flush();
        em.clear();

        service.deleteCategories(List.of(c1.getId(), c2.getId()));
        em.flush();
        em.clear();

        assertThat(categoryRepository.findAll()).isEmpty();
    }

    @Test
    void shouldThrowBadRequestWhenDeleteCategoriesBulkWithBooks() {
        var c1 = saveCategory("C1");
        var c2 = saveCategory("C2");
        saveBook("Book", c1, Set.of());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.deleteCategories(List.of(c1.getId(), c2.getId())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.category.has_books");
    }


    private Category saveCategory(String name) {
        var category = Category.builder()
                .popularityCount(0)
                .build();
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name(name)
                .description("Desc " + name)
                .category(category)
                .build();
        category.setTranslations(new HashMap<>(Map.of("en", translation)));
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
                .country("USA")
                .biography("Biography of " + fullName)
                .author(author)
                .build();
        author.setTranslations(new HashMap<>(Map.of("en", translation)));
        return authorRepository.save(author);
    }

    private Book saveBook(String title, Category category, Set<Author> authors) {
        var book = Book.builder()
                .category(category)
                .authors(authors)
                .publishYear((short) 2000)
                .pages((short) 200)
                .status(BookStatus.PRELIMINARY)
                .popularityCount(0)
                .build();
        var translation = BookTranslation.builder()
                .languageCode("en")
                .title(title)
                .bookLanguage("English")
                .description("Desc " + title)
                .book(book)
                .build();
        book.setTranslations(new HashMap<>(Map.of("en", translation)));
        return bookRepository.save(book);
    }

}
