package org.example.library.collection_book.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.collection.domain.Collection;
import org.example.library.collection.repository.CollectionRepository;
import org.example.library.collection_book.dto.CollectionBookSearchParams;
import org.example.library.collection_book.repository.CollectionBookRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.common.exception.BadRequestException;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.user.domain.Role;
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
class CollectionBookServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CollectionBookRepository repository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CollectionBookService service;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }


    @Test
    void shouldAddBookToCollection() {
        var user = saveUser("user@example.com");
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var collection = saveCollection(user, "My Collection");
        em.flush();
        em.clear();

        service.addBookToCollection(user.getId(), collection.getId(), libraryBook.getId());

        var result = repository.findLibraryBookIdsByCollectionId(collection.getId());
        assertThat(result).containsExactly(libraryBook.getId());
    }

    @Test
    void shouldThrowBadRequestWhenAddingToAnotherUsersCollection() {
        var user1 = saveUser("user1@example.com");
        var user2 = saveUser("user2@example.com");
        var book = saveBook();
        var libraryBook = saveLibraryBook(user1, book);
        var collection = saveCollection(user2, "User 2 Collection");
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.addBookToCollection(user1.getId(), collection.getId(), libraryBook.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.collection.not_belong_to_user");
    }

    @Test
    void shouldBulkAddBooksToCollection() {
        var user = saveUser("user@example.com");
        var book1 = saveBook();
        var book2 = saveBook();
        var lb1 = saveLibraryBook(user, book1);
        var lb2 = saveLibraryBook(user, book2);
        var collection = saveCollection(user, "Bulk Collection");
        em.flush();
        em.clear();

        service.bulkAddBooksToCollection(user.getId(), collection.getId(), List.of(lb1.getId(), lb2.getId()));

        var result = repository.findLibraryBookIdsByCollectionId(collection.getId());
        assertThat(result).containsExactlyInAnyOrder(lb1.getId(), lb2.getId());
    }

    @Test
    void shouldGetCollectionBooksPaginated() {
        var user = saveUser("user@example.com");
        var book1 = saveBook();
        var book2 = saveBook();
        var lb1 = saveLibraryBook(user, book1);
        var lb2 = saveLibraryBook(user, book2);
        var collection = saveCollection(user, "Paginated Collection");
        service.addBookToCollection(user.getId(), collection.getId(), lb1.getId());
        service.addBookToCollection(user.getId(), collection.getId(), lb2.getId());
        em.flush();
        em.clear();
        var searchParams = new CollectionBookSearchParams();
        var paginationParams = new PaginationParams();
        paginationParams.setPage(0);
        paginationParams.setSize(10);

        var result = service.getCollectionBooksPaginated(user.getId(), collection.getId(), searchParams, paginationParams);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void shouldRemoveBookFromCollection() {
        var user = saveUser("user@example.com");
        var book = saveBook();
        var lb = saveLibraryBook(user, book);
        var collection = saveCollection(user, "Removal Collection");
        service.addBookToCollection(user.getId(), collection.getId(), lb.getId());
        em.flush();
        em.clear();

        service.removeBookFromCollection(user.getId(), collection.getId(), lb.getId());

        assertThat(repository.findLibraryBookIdsByCollectionId(collection.getId())).isEmpty();
    }

    @Test
    void shouldBulkRemoveBooksFromCollection() {
        var user = saveUser("user@example.com");
        var lb1 = saveLibraryBook(user, saveBook());
        var lb2 = saveLibraryBook(user, saveBook());
        var collection = saveCollection(user, "Bulk Removal");
        service.bulkAddBooksToCollection(user.getId(), collection.getId(), List.of(lb1.getId(), lb2.getId()));
        em.flush();
        em.clear();

        service.bulkRemoveBooksFromCollection(user.getId(), collection.getId(), List.of(lb1.getId(), lb2.getId()));

        assertThat(repository.findLibraryBookIdsByCollectionId(collection.getId())).isEmpty();
    }

    @Test
    void shouldRemoveBookFromAllCollections() {
        var user = saveUser("user@example.com");
        var lb = saveLibraryBook(user, saveBook());
        var col1 = saveCollection(user, "Col 1");
        var col2 = saveCollection(user, "Col 2");
        service.addBookToCollection(user.getId(), col1.getId(), lb.getId());
        service.addBookToCollection(user.getId(), col2.getId(), lb.getId());
        em.flush();
        em.clear();

        service.removeBookFromAllCollections(user.getId(), lb.getId());

        assertThat(repository.findLibraryBookIdsByCollectionId(col1.getId())).isEmpty();
        assertThat(repository.findLibraryBookIdsByCollectionId(col2.getId())).isEmpty();
    }


    private User saveUser(String email) {
        var user = User.builder()
                .email(email)
                .fullName("User")
                .password("pass")
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    private Category saveCategory() {
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name("Default Category")
                .description("Description of " + "Default Category")
                .build();
        var category = Category.builder()
                .popularityCount(0)
                .translations(Map.of("en", translation))
                .build();
        translation.setCategory(category);

        return categoryRepository.save(category);
    }

    private Book saveBook() {
        var category = saveCategory();
        var book = Book.builder()
                .category(category)
                .status(BookStatus.NEW)
                .popularityCount(0)
                .build();

        return bookRepository.save(book);
    }

    private LibraryBook saveLibraryBook(User user, Book book) {
        var libraryBook = LibraryBook.builder()
                .user(user)
                .book(book)
                .build();

        return libraryBookRepository.save(libraryBook);
    }

    private Collection saveCollection(User user, String name) {
        var collection = Collection.builder()
                .user(user)
                .name(name)
                .build();

        return collectionRepository.save(collection);
    }

}
