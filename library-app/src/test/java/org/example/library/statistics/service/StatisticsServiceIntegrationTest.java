package org.example.library.statistics.service;

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
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Transactional
class StatisticsServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private StatisticsService service;


    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }


    @Test
    void shouldReturnDashboardStats() {
        var user = User.builder().email("stats_test@example.com").fullName("Stats Test User").password("pass").build();
        userRepository.save(user);
        var category = saveCategory();
        var author = saveAuthor();
        var book = saveBook(category, author);
        var lb1 = LibraryBook.builder()
                .user(user)
                .book(book)
                .status(LibraryBookStatus.READ)
                .finishedAt(LocalDate.of(2023, 5, 10))
                .rating((byte) 5)
                .pages((short) 300)
                .language("English")
                .build();
        libraryBookRepository.save(lb1);
        var lb2 = LibraryBook.builder()
                .user(user)
                .book(book)
                .status(LibraryBookStatus.READING)
                .build();
        libraryBookRepository.save(lb2);
        var lb3 = LibraryBook.builder()
                .user(user)
                .book(book)
                .status(LibraryBookStatus.READ)
                .finishedAt(LocalDate.of(2023, 11, 15))
                .rating((byte) 4)
                .build();
        libraryBookRepository.save(lb3);
        em.flush();
        em.clear();

        var stats = service.getDashboardStats(user.getId(), 2023);

        assertThat(stats).isNotNull();
        var summary = stats.getSummary();
        assertThat(summary.getTotalLibraryBooks()).isEqualTo(3L);
        assertThat(summary.getBooksReadCount()).isEqualTo(2L);
        assertThat(summary.getPagesReadCount()).isEqualTo(600L);
        assertThat(summary.getAverageRating()).isEqualTo(4.5);
        assertThat(summary.getCurrentlyReadingCount()).isEqualTo(1L);
        assertThat(summary.getTotalRatedBooks()).isEqualTo(2L);
        assertThat(stats.getCategoryDistribution()).hasSize(1);
        assertThat(stats.getCategoryDistribution().get(0).getCategoryName()).isEqualTo("Fiction");
        assertThat(stats.getCategoryDistribution().get(0).getCount()).isEqualTo(3L);
        assertThat(stats.getStatusDistribution()).hasSize(2);
        assertThat(stats.getStatusDistribution())
                .filteredOn(s -> s.getStatus() == LibraryBookStatus.READ)
                .extracting("count")
                .containsExactly(2L);
        assertThat(stats.getStatusDistribution())
                .filteredOn(s -> s.getStatus() == LibraryBookStatus.READING)
                .extracting("count")
                .containsExactly(1L);
        assertThat(stats.getLanguageDistribution()).hasSize(1);
        assertThat(stats.getLanguageDistribution())
                .extracting("language", "count")
                .containsExactly(tuple("English", 3L));
        assertThat(stats.getAuthorCountryDistribution()).hasSize(1);
        assertThat(stats.getAuthorCountryDistribution())
                .extracting("country", "count")
                .containsExactly(tuple("UK", 3L));
        assertThat(stats.getMonthlyReadingActivity()).hasSize(2);
        assertThat(stats.getMonthlyReadingActivity())
                .filteredOn(m -> m.getMonth() == 5)
                .extracting("count")
                .containsExactly(1L);
        assertThat(stats.getMonthlyReadingActivity())
                .filteredOn(m -> m.getMonth() == 11)
                .extracting("count")
                .containsExactly(1L);
        assertThat(stats.getTopAuthors()).hasSize(1);
        assertThat(stats.getTopAuthors())
                .extracting("authorName", "count")
                .containsExactly(tuple("Author 1", 3L));
    }


    private Category saveCategory() {
        var translation = CategoryTranslation.builder()
                .languageCode("en")
                .name("Fiction")
                .description("Description of " + "Fiction")
                .build();
        var category = Category.builder()
                .popularityCount(0)
                .translations(Map.of("en", translation))
                .build();
        translation.setCategory(category);

        return categoryRepository.save(category);
    }

    private Author saveAuthor() {
        var author = Author.builder()
                .birthYear((short) 1970)
                .popularityCount(0)
                .build();
        authorRepository.save(author);
        var translation = AuthorTranslation.builder()
                .authorId(author.getId())
                .languageCode("en")
                .fullName("Author 1")
                .country("UK")
                .author(author)
                .build();
        author.setTranslations(Map.of("en", translation));

        return author;
    }

    private Book saveBook(Category category, Author author) {
        var book = Book.builder()
                .category(category)
                .pages((short) 300)
                .popularityCount(0)
                .authors(Set.of(author))
                .build();
        bookRepository.save(book);
        var translation = BookTranslation.builder()
                .bookId(book.getId())
                .languageCode("en")
                .title("Title")
                .bookLanguage("English")
                .book(book)
                .build();
        book.setTranslations(Map.of("en", translation));

        return book;
    }

}
