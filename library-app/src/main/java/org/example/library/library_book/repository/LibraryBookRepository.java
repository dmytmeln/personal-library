package org.example.library.library_book.repository;

import org.example.library.book.dto.LanguageWithCount;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.dto.BookRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Integer> {

    @Query("""
            SELECT
                COALESCE(lb.language, tr.bookLanguage) AS language,
                COUNT(lb) AS count
            FROM LibraryBook lb
            JOIN lb.book b
            LEFT JOIN b.translations tr ON tr.languageCode = :lang
            WHERE lb.user.id = :userId
            GROUP BY COALESCE(lb.language, tr.bookLanguage)
            ORDER BY COUNT(lb) DESC
            """)
    List<LanguageWithCount> findLanguagesWithCountByUserId(Integer userId, String lang);

    @Query("""
            SELECT
                AVG(lb.rating) AS averageRating,
                COUNT(lb) AS ratingsCount
            FROM LibraryBook lb
            WHERE lb.book.id = :bookId AND lb.rating IS NOT NULL
            """)
    BookRatingSummary findAverageRatingAndCountByBookId(Integer bookId);

    @Query("SELECT lb.book.id FROM LibraryBook lb WHERE lb.user.id = :userId AND lb.book.id IN :bookIds")
    List<Integer> findExistingBookIdsInLibrary(Integer userId, List<Integer> bookIds);

    @Modifying
    @Query("UPDATE LibraryBook lb SET lb.rating = :rating WHERE lb.id = :id AND lb.user.id = :userId")
    int updateRating(Integer id, Integer userId, Byte rating);

    @Modifying
    @Query("""
            UPDATE LibraryBook lb
            SET lb.title = NULL,
                lb.publishYear = NULL,
                lb.pages = NULL,
                lb.language = NULL,
                lb.description = NULL
            WHERE lb.id = :id
            """)
    int resetOverriddenFields(Integer id, Integer userId);

    Optional<LibraryBook> findByIdAndUserId(Integer libraryBookId, Integer userId);

    @Query("SELECT lb FROM LibraryBook lb JOIN FETCH lb.book LEFT JOIN FETCH lb.book.owner WHERE lb.id = :libraryBookId AND lb.user.id = :userId")
    Optional<LibraryBook> findByIdAndUserIdWithBook(Integer libraryBookId, Integer userId);

    boolean existsByIdAndUserId(Integer libraryBookId, Integer userId);

    boolean existsByBookIdAndUserId(Integer bookId, Integer userId);

    @Query("SELECT lb FROM LibraryBook lb JOIN lb.book b WHERE lb.user.id = :userId")
    List<LibraryBook> findAllWithVectorsByUserId(Integer userId);

    List<LibraryBook> findAllByIdInAndUserId(List<Integer> ids, Integer userId);

    @Query("SELECT lb FROM LibraryBook lb JOIN FETCH lb.book WHERE lb.id IN :ids AND lb.user.id = :userId")
    List<LibraryBook> findAllByIdInAndUserIdWithBook(List<Integer> ids, Integer userId);

    List<LibraryBook> findAllByUserId(Integer userId);

}
