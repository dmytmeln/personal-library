package org.example.library.quote.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.repository.BookRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.quote.domain.Quote;
import org.example.library.quote.dto.QuoteRequest;
import org.example.library.quote.repository.QuoteRepository;
import org.example.library.user.domain.Role;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class QuoteServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private QuoteRepository repository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private QuoteService service;

    @Test
    void shouldGetQuotesByLibraryBookId() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var quote1 = Quote.builder()
                .text("Quote 1")
                .libraryBook(libraryBook)
                .build();
        var quote2 = Quote.builder()
                .text("Quote 2")
                .libraryBook(libraryBook)
                .build();
        repository.saveAll(List.of(quote1, quote2));
        em.flush();
        em.clear();

        var result = service.getByLibraryBookId(libraryBook.getId(), user.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting("text").containsExactlyInAnyOrder("Quote 1", "Quote 2");
    }

    @Test
    void shouldCreateNewQuote() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var request = new QuoteRequest("New Quote Text", "42", "Good one");

        var result = service.create(libraryBook.getId(), request, user.getId());

        assertThat(result.id()).isNotNull();
        assertThat(result.text()).isEqualTo("New Quote Text");
        assertThat(result.page()).isEqualTo("42");
        assertThat(result.comment()).isEqualTo("Good one");
        
        var savedQuotes = repository.findByLibraryBookIdAndLibraryBookUserIdOrderByCreatedAtDesc(libraryBook.getId(), user.getId());
        assertThat(savedQuotes).hasSize(1);
        assertThat(savedQuotes.get(0).getText()).isEqualTo("New Quote Text");
    }

    @Test
    void shouldUpdateExistingQuote() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var quote = Quote.builder()
                .text("Old Text")
                .libraryBook(libraryBook)
                .build();
        repository.save(quote);
        em.flush();
        em.clear();
        var request = new QuoteRequest("Updated Text", "10", "Updated comment");

        var result = service.update(quote.getId(), request, user.getId());

        assertThat(result.id()).isEqualTo(quote.getId());
        assertThat(result.text()).isEqualTo("Updated Text");
        
        var updatedQuote = repository.findById(quote.getId()).orElseThrow();
        assertThat(updatedQuote.getText()).isEqualTo("Updated Text");
        assertThat(updatedQuote.getPage()).isEqualTo("10");
    }

    @Test
    void shouldDeleteQuote() {
        var user = saveUser();
        var book = saveBook();
        var libraryBook = saveLibraryBook(user, book);
        var quote = Quote.builder()
                .text("To delete")
                .libraryBook(libraryBook)
                .build();
        repository.save(quote);
        em.flush();
        em.clear();

        service.delete(quote.getId(), user.getId());

        assertThat(repository.findById(quote.getId())).isEmpty();
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingSomeoneElsesQuote() {
        var user1 = saveUser("user1@example.com");
        var user2 = saveUser("user2@example.com");
        var book = saveBook();
        var libraryBook = saveLibraryBook(user1, book);
        var quote = Quote.builder()
                .text("User 1 Quote")
                .libraryBook(libraryBook)
                .build();
        repository.save(quote);
        em.flush();
        em.clear();
        
        var request = new QuoteRequest("Hack", "1", "Hacked");

        assertThatThrownBy(() -> service.update(quote.getId(), request, user2.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.quote.not_found");
    }

    private User saveUser() {
        return saveUser("user@example.com");
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
