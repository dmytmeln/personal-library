package org.example.library.library_book.repository;

import org.example.library.library_book.domain.LibraryBookView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

}
