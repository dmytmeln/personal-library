package org.example.library.note.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.note.domain.Note;
import org.example.library.note.dto.NoteRequest;
import org.example.library.note.repository.NoteRepository;
import org.example.library.user.domain.Role;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class NoteServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private NoteRepository repository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private NoteService service;


    @Test
    void shouldGetNoteByLibraryBookId() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var note = Note.builder()
                .content("Sample Note")
                .libraryBook(libraryBook)
                .build();
        repository.save(note);
        em.flush();
        em.clear();

        var result = service.getByLibraryBookId(libraryBook.getId(), user.getId());

        assertThat(result.content()).isEqualTo("Sample Note");
    }

    @Test
    void shouldThrowNotFoundWhenNoteDoesNotExist() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);

        assertThatThrownBy(() -> service.getByLibraryBookId(libraryBook.getId(), user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.note.not_found");
    }

    @Test
    void shouldCreateNewNote() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var request = new NoteRequest(libraryBook.getId(), "New Note Content");

        var result = service.createOrUpdate(request, user.getId());

        assertThat(result.id()).isNotNull();
        assertThat(result.content()).isEqualTo("New Note Content");
        var savedNote = repository.findByLibraryBookIdAndLibraryBookUserId(libraryBook.getId(), user.getId())
                .orElseThrow(() -> new AssertionError("Note should have been saved"));
        assertThat(savedNote.getContent()).isEqualTo("New Note Content");
    }

    @Test
    void shouldUpdateExistingNote() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var note = Note.builder()
                .content("Old Content")
                .libraryBook(libraryBook)
                .build();
        repository.save(note);
        em.flush();
        em.clear();
        var request = new NoteRequest(libraryBook.getId(), "Updated Content");

        var result = service.createOrUpdate(request, user.getId());

        assertThat(result.id()).isEqualTo(note.getId());
        assertThat(result.content()).isEqualTo("Updated Content");
        var updatedNote = repository.findById(note.getId())
                .orElseThrow(() -> new AssertionError("Note not found after update"));
        assertThat(updatedNote.getContent()).isEqualTo("Updated Content");
    }

    @Test
    void shouldDeleteNote() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var note = Note.builder()
                .content("Note to delete")
                .libraryBook(libraryBook)
                .build();
        repository.save(note);
        em.flush();
        em.clear();

        service.delete(libraryBook.getId(), user.getId());

        assertThat(repository.findByLibraryBookIdAndLibraryBookUserId(libraryBook.getId(), user.getId())).isEmpty();
    }


    private User saveUser() {
        var user = User.builder()
                .email("user@example.com")
                .fullName("User")
                .password("pass")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private Book saveBook() {
        var book = Book.builder()
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

}
