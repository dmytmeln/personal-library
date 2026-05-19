package org.example.library.book.repository;

import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.dto.LanguageWithCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> {

    @Query("""
            SELECT
                tr.bookLanguage AS language,
                COUNT(b) AS count
            FROM Book b
            JOIN b.translations tr ON tr.languageCode = :lang
            WHERE b.owner IS NULL
            GROUP BY tr.bookLanguage
            ORDER BY COUNT(b) DESC
            """)
    List<LanguageWithCount> findAllLanguagesWithCount(String lang);

    @Query("SELECT b FROM Book b WHERE b.id = :id AND b.owner IS NULL AND b.embedding IS NOT NULL")
    Optional<Book> findEmbeddingById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"category"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Book> findAll(@Nullable Specification<Book> spec, Pageable pageable);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.status <> :status AND b.owner IS NULL")
    long countWhereBookStatusNot(BookStatus status);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.owner IS NULL AND b.embedding IS NULL")
    long countBooksWithoutEmbedding();

    @EntityGraph(attributePaths = {"category", "translations", "category.translations"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT b FROM Book b WHERE b.owner IS NULL AND b.embedding IS NULL")
    Page<Book> findBooksWithoutEmbedding(Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET b.popularityCount = b.popularityCount + 1 WHERE b.id IN :ids")
    void incrementPopularityCount(List<Integer> ids);

    @Modifying
    @Query("UPDATE Book b SET b.popularityCount = b.popularityCount - 1 WHERE b.id IN :ids")
    void decrementPopularityCount(List<Integer> ids);

    boolean existsByAuthorsId(Integer authorId);

    boolean existsByCategoryId(Integer categoryId);

}
