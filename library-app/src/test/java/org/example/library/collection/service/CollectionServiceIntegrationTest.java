package org.example.library.collection.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.book.domain.Book;
import org.example.library.book.repository.BookRepository;
import org.example.library.collection.domain.Collection;
import org.example.library.collection.dto.CreateCollectionRequest;
import org.example.library.collection.dto.UpdateCollectionDto;
import org.example.library.collection.repository.CollectionRepository;
import org.example.library.collection_book.domain.CollectionBook;
import org.example.library.collection_book.domain.CollectionBookId;
import org.example.library.collection_book.repository.CollectionBookRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.common.exception.BadRequestException;
import org.example.library.common.exception.NotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CollectionServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private CollectionBookRepository collectionBookRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CollectionService service;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }


    @Test
    void shouldReturnAllCollectionsForUser() {
        var user = saveUser();
        saveCollection("Collection 1", user);
        saveCollection("Collection 2", user);
        em.flush();
        em.clear();

        var result = service.getAllCollections(user.getId(), null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactlyInAnyOrder("Collection 1", "Collection 2");
    }

    @Test
    void shouldReturnCollectionsByUserIdAndBookId() {
        var user = saveUser();
        var collection = saveCollection("My Collection", user);
        var book = saveBook();
        var libraryBook = saveLibraryBook(book, user);
        saveCollectionBook(collection, libraryBook);
        em.flush();
        em.clear();

        var result = service.getAllByUserIdAndBookId(user.getId(), book.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Collection");
    }

    @Test
    void shouldReturnUserCollectionTree() {
        var user = saveUser();
        var root = saveCollection("Root", user);
        var child = saveCollection("Child", user);
        child.setParent(root);
        collectionRepository.save(child);
        em.flush();
        em.clear();

        var result = service.getUserCollectionTree(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Root");
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getName()).isEqualTo("Child");
    }

    @Test
    void shouldReturnCollectionDetailsWithAncestors() {
        var user = saveUser();
        var root = saveCollection("Root", user);
        var child = saveCollection("Child", user);
        child.setParent(root);
        collectionRepository.save(child);
        em.flush();
        em.clear();

        var result = service.getCollectionDetails(child.getId(), user.getId());

        assertThat(result.getName()).isEqualTo("Child");
        assertThat(result.getAncestors()).hasSize(1);
        assertThat(result.getAncestors().get(0).getName()).isEqualTo("Root");
    }

    @Test
    void shouldThrowNotFoundWhenGettingNonExistentCollectionDetails() {
        var user = saveUser();
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.getCollectionDetails(-1, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.collection.not_found");
    }

    @Test
    void shouldCreateCollection() {
        var user = saveUser();
        var request = new CreateCollectionRequest();
        request.setName("New Collection");
        request.setDescription("Description");
        em.flush();
        em.clear();

        var result = service.createCollection(request, user.getId());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("New Collection");
        assertThat(collectionRepository.existsById(result.getId())).isTrue();
    }

    @Test
    void shouldCreateSubCollection() {
        var user = saveUser();
        var parent = saveCollection("Parent", user);
        var request = new CreateCollectionRequest();
        request.setName("Sub");
        request.setParentId(parent.getId());
        em.flush();
        em.clear();

        var result = service.createCollection(request, user.getId());

        assertThat(result.getName()).isEqualTo("Sub");
        var saved = collectionRepository.findById(result.getId())
                .orElseThrow(() -> new AssertionError("Collection not found after save"));
        assertThat(saved.getParent().getId()).isEqualTo(parent.getId());
    }

    @Test
    void shouldThrowBadRequestWhenCreatingCollectionExceedingMaxDepth() {
        var user = saveUser();
        var c1 = saveCollection("c1", user);
        var c2 = saveCollection("c2", user);
        c2.setParent(c1);
        var c3 = saveCollection("c3", user);
        c3.setParent(c2);
        var c4 = saveCollection("c4", user);
        c4.setParent(c3);
        collectionRepository.saveAll(java.util.List.of(c2, c3, c4));
        var request = new CreateCollectionRequest();
        request.setName("c5");
        request.setParentId(c4.getId());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.createCollection(request, user.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.collection.max_depth_exceeded");
    }

    @Test
    void shouldUpdateCollection() {
        var user = saveUser();
        var collection = saveCollection("Old Name", user);
        var dto = new UpdateCollectionDto();
        dto.setName("New Name");
        em.flush();
        em.clear();

        var result = service.updateCollection(collection.getId(), dto, user.getId());

        assertThat(result.getName()).isEqualTo("New Name");
        var saved = collectionRepository.findById(collection.getId())
                .orElseThrow(() -> new AssertionError("Collection not found after update"));
        assertThat(saved.getName()).isEqualTo("New Name");
    }

    @Test
    void shouldMoveCollection() {
        var user = saveUser();
        var parent1 = saveCollection("Parent 1", user);
        var parent2 = saveCollection("Parent 2", user);
        var child = saveCollection("Child", user);
        child.setParent(parent1);
        collectionRepository.save(child);
        em.flush();
        em.clear();

        service.moveCollection(child.getId(), parent2.getId(), user.getId());

        var saved = collectionRepository.findById(child.getId())
                .orElseThrow(() -> new AssertionError("Collection not found after move"));
        assertThat(saved.getParent().getId()).isEqualTo(parent2.getId());
    }

    @Test
    void shouldMakeCollectionRootWhenMovingToNullParent() {
        var user = saveUser();
        var parent = saveCollection("Parent", user);
        var child = saveCollection("Child", user);
        child.setParent(parent);
        collectionRepository.save(child);
        em.flush();
        em.clear();

        service.moveCollection(child.getId(), null, user.getId());

        var saved = collectionRepository.findById(child.getId())
                .orElseThrow(() -> new AssertionError("Collection not found after moving to root"));
        assertThat(saved.getParent()).isNull();
    }

    @Test
    void shouldThrowBadRequestWhenMovingCollectionToItself() {
        var user = saveUser();
        var collection = saveCollection("Collection", user);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.moveCollection(collection.getId(), collection.getId(), user.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.collection.cannot_be_own_parent");
    }

    @Test
    void shouldDeleteCollection() {
        var user = saveUser();
        var collection = saveCollection("To Delete", user);
        em.flush();
        em.clear();

        service.deleteCollection(collection.getId(), user.getId());

        assertThat(collectionRepository.existsById(collection.getId())).isFalse();
    }

    @Test
    void shouldMoveBookBetweenCollections() {
        var user = saveUser();
        var source = saveCollection("Source", user);
        var target = saveCollection("Target", user);
        var book = saveBook();
        var libraryBook = saveLibraryBook(book, user);
        saveCollectionBook(source, libraryBook);
        em.flush();
        em.clear();

        service.moveBook(source.getId(), target.getId(), libraryBook.getId(), user.getId());

        assertThat(collectionBookRepository.existsById(new CollectionBookId(source.getId(), libraryBook.getId()))).isFalse();
        assertThat(collectionBookRepository.existsById(new CollectionBookId(target.getId(), libraryBook.getId()))).isTrue();
    }


    private User saveUser() {
        var user = User.builder()
                .email("user@test.com")
                .fullName("Test User")
                .password("password")
                .build();
        return userRepository.save(user);
    }

    private Collection saveCollection(String name, User user) {
        var collection = Collection.builder()
                .name(name)
                .user(user)
                .build();
        return collectionRepository.save(collection);
    }

    private Book saveBook() {
        var book = Book.builder()
                .popularityCount(0)
                .build();
        return bookRepository.save(book);
    }

    private LibraryBook saveLibraryBook(Book book, User user) {
        var libraryBook = LibraryBook.builder()
                .book(book)
                .user(user)
                .title("Test Library Book")
                .build();
        return libraryBookRepository.save(libraryBook);
    }

    private void saveCollectionBook(Collection collection, LibraryBook libraryBook) {
        var collectionBook = CollectionBook.builder()
                .id(new CollectionBookId(collection.getId(), libraryBook.getId()))
                .collection(collection)
                .libraryBook(libraryBook)
                .build();
        collectionBookRepository.save(collectionBook);
    }

}
