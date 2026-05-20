package org.example.library.author.repository;

import org.example.library.author.domain.Author;
import org.example.library.author.dto.AuthorWithBooksCount;
import org.example.library.author.dto.CountryWithCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Integer> {

    @Query("""
            SELECT
                a.id AS id,
                tr.fullName AS fullName,
                tr.country AS country,
                a.birthYear AS birthYear,
                a.deathYear AS deathYear,
                a.popularityCount AS popularityCount,
                COUNT(b) AS booksCount
            FROM Author a
            JOIN a.translations tr ON tr.languageCode = :lang
            LEFT JOIN a.books b
            WHERE (:name IS NULL OR (LOWER(tr.fullName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')) OR FUNCTION('similarity', tr.fullName, CAST(:name AS string)) > 0.3))
              AND (:country IS NULL OR LOWER(tr.country) = LOWER(CAST(:country AS string)))
              AND (:birthYearMin IS NULL OR a.birthYear >= :birthYearMin)
              AND (:birthYearMax IS NULL OR a.birthYear <= :birthYearMax)
            GROUP BY a.id, tr.fullName, tr.country, a.birthYear, a.deathYear
            HAVING (:booksCountMin IS NULL OR COUNT(b) >= :booksCountMin)
               AND (:booksCountMax IS NULL OR COUNT(b) <= :booksCountMax)
            """)
    Page<AuthorWithBooksCount> searchWithBooksCount(
            String name,
            String country,
            Short birthYearMin,
            Short birthYearMax,
            Integer booksCountMin,
            Integer booksCountMax,
            String lang,
            Pageable pageable
    );

    @Query("""
            SELECT
                tr.country AS country,
                COUNT(a) AS count
            FROM Author a
            JOIN a.translations tr ON tr.languageCode = :lang
            GROUP BY tr.country
            ORDER BY COUNT(a) DESC
            """)
    List<CountryWithCount> findAllCountriesWithCount(String lang);

    @Query("""
            SELECT
                a.id AS id,
                tr.fullName AS fullName,
                tr.country AS country,
                a.birthYear AS birthYear,
                a.deathYear AS deathYear,
                a.popularityCount AS popularityCount,
                COUNT(DISTINCT lb.id) AS booksCount
            FROM Author a
            JOIN a.translations tr ON tr.languageCode = :lang
            JOIN a.books b
            JOIN LibraryBook lb ON lb.book.id = b.id
            WHERE lb.user.id = :userId
              AND (:name IS NULL OR (LOWER(tr.fullName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')) OR FUNCTION('similarity', tr.fullName, CAST(:name AS string)) > 0.3))
              AND (:country IS NULL OR LOWER(tr.country) = LOWER(CAST(:country AS string)))
              AND (:birthYearMin IS NULL OR a.birthYear >= :birthYearMin)
              AND (:birthYearMax IS NULL OR a.birthYear <= :birthYearMax)
            GROUP BY a.id, tr.fullName, tr.country, a.birthYear, a.deathYear
            HAVING (:booksCountMin IS NULL OR COUNT(DISTINCT lb.id) >= :booksCountMin)
               AND (:booksCountMax IS NULL OR COUNT(DISTINCT lb.id) <= :booksCountMax)
            """)
    Page<AuthorWithBooksCount> searchForUser(
            Integer userId,
            String name,
            String country,
            Short birthYearMin,
            Short birthYearMax,
            Integer booksCountMin,
            Integer booksCountMax,
            String lang,
            Pageable pageable
    );

    @Query("""
            SELECT
                tr.country AS country,
                COUNT(DISTINCT a.id) AS count
            FROM Author a
            JOIN a.translations tr ON tr.languageCode = :lang
            JOIN a.books b
            JOIN LibraryBook lb ON lb.book.id = b.id
            WHERE lb.user.id = :userId
            GROUP BY tr.country
            ORDER BY COUNT(DISTINCT a.id) DESC
            """)
    List<CountryWithCount> findAllCountriesForUser(Integer userId, String lang);

    @Modifying
    @Query(value = "UPDATE authors SET popularity_count = popularity_count + 1 WHERE author_id IN (SELECT author_id FROM book_authors WHERE book_id IN :bookIds)", nativeQuery = true)
    void incrementPopularityCountByBookIds(List<Integer> bookIds);

    @Modifying
    @Query(value = "UPDATE authors SET popularity_count = popularity_count - 1 WHERE author_id IN (SELECT author_id FROM book_authors WHERE book_id IN :bookIds)", nativeQuery = true)
    void decrementPopularityCountByBookIds(List<Integer> bookIds);

}
