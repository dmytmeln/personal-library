package org.example.library.category.repository;

import org.example.library.category.domain.Category;
import org.example.library.category.dto.CategoryWithBooksCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    @Query("SELECT c.id FROM Category c")
    Set<Integer> findAllIds();

    @Query("""
            SELECT
                c.id AS id,
                tr.name AS name,
                tr.description AS description,
                c.popularityCount AS popularityCount,
                COUNT(b) AS booksCount
            FROM Category c
            JOIN c.translations tr ON tr.languageCode = :lang
            LEFT JOIN c.books b
            WHERE (:name IS NULL OR (LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')) OR FUNCTION('similarity', tr.name, CAST(:name AS string)) > 0.3))
            GROUP BY c.id, tr.name, tr.description, c.popularityCount
            HAVING (:booksCountMin IS NULL OR COUNT(b) >= :booksCountMin)
               AND (:booksCountMax IS NULL OR COUNT(b) <= :booksCountMax)
            """)
    Page<CategoryWithBooksCount> searchWithBooksCount(
            String name,
            Integer booksCountMin,
            Integer booksCountMax,
            String lang,
            Pageable pageable
    );

    @Query("""
            SELECT
                c.id AS id,
                tr.name AS name,
                tr.description AS description,
                c.popularityCount AS popularityCount,
                COUNT(DISTINCT lb.id) AS booksCount
            FROM Category c
            JOIN c.translations tr ON tr.languageCode = :lang
            JOIN c.books b
            JOIN LibraryBook lb ON lb.book.id = b.id
            WHERE lb.user.id = :userId
              AND (:name IS NULL OR (LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')) OR FUNCTION('similarity', tr.name, CAST(:name AS string)) > 0.3))
            GROUP BY c.id, tr.name, tr.description, c.popularityCount
            HAVING (:booksCountMin IS NULL OR COUNT(DISTINCT lb.id) >= :booksCountMin)
               AND (:booksCountMax IS NULL OR COUNT(DISTINCT lb.id) <= :booksCountMax)
            """)
    Page<CategoryWithBooksCount> searchForUser(
            Integer userId,
            String name,
            Integer booksCountMin,
            Integer booksCountMax,
            String lang,
            Pageable pageable
    );

    @Modifying
    @Query(value = "UPDATE categories SET popularity_count = popularity_count + 1 WHERE category_id IN (SELECT category_id FROM books WHERE book_id IN :bookIds)", nativeQuery = true)
    void incrementPopularityCountByBookIds(List<Integer> bookIds);

    @Modifying
    @Query(value = "UPDATE categories SET popularity_count = popularity_count - 1 WHERE category_id IN (SELECT category_id FROM books WHERE book_id IN :bookIds)", nativeQuery = true)
    void decrementPopularityCountByBookIds(List<Integer> bookIds);

}
