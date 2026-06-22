package org.example.library.library_book.repository;

import org.example.library.library_book.domain.LibraryBookView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryBookViewRepository extends JpaRepository<LibraryBookView, Integer>, JpaSpecificationExecutor<LibraryBookView>, LibraryBookViewRepositoryCustom {

    @Query("""
            SELECT v
            FROM LibraryBookView v
            LEFT JOIN FETCH v.authors
            WHERE v.id = :id
                AND (v.languageCode = :languageCode OR v.languageCode IS NULL)
            """)
    Optional<LibraryBookView> findByIdAndLanguageCode(Integer id, String languageCode);

    @Query("""
            SELECT v
            FROM LibraryBookView v
            LEFT JOIN FETCH v.authors
            WHERE v.bookId = :bookId
                AND v.userId = :userId
                AND (v.languageCode = :languageCode OR v.languageCode IS NULL)
            """)
    Optional<LibraryBookView> findByBookIdAndUserIdAndLanguageCode(Integer bookId, Integer userId, String languageCode);

    @Query(value = """
            SELECT lbv.*
            FROM library_books_view lbv
            JOIN books b ON lbv.book_id = b.book_id
            WHERE lbv.user_id = :userId
              AND (lbv.language_code = :languageCode OR lbv.language_code IS NULL)
              AND b.embedding IS NOT NULL
              AND (:status IS NULL OR lbv.status = :status)
            ORDER BY b.embedding <=> cast(:vector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<LibraryBookView> searchByMood(float[] vector, String languageCode, Integer userId, String status, int limit);

}
