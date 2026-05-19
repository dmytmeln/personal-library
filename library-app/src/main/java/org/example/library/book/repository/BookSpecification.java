package org.example.library.book.repository;

import jakarta.persistence.criteria.JoinType;
import org.example.library.author.domain.Author_;
import org.example.library.book.domain.BookDisplayView;
import org.example.library.book.domain.BookDisplayView_;
import org.example.library.book.dto.BookSearchParams;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class BookSpecification {

    public static Specification<BookDisplayView> fromSearchParams(String lang, BookSearchParams searchParams) {
        return Specification.where(hasLanguageCode(lang))
                .and(hasCategoryId(searchParams.getCategoryId()))
                .and(hasAuthorId(searchParams.getAuthorId()))
                .and(hasTitleLike(searchParams.getTitle()))
                .and(hasPublishYearBetween(searchParams.getPublishYearMin(), searchParams.getPublishYearMax()))
                .and(hasLanguageIn(searchParams.getLanguages()))
                .and(hasPagesBetween(searchParams.getPagesMin(), searchParams.getPagesMax()));
    }

    public static Specification<BookDisplayView> hasLanguageCode(String lang) {
        return (root, query, cb) -> {
            if (lang == null)
                return null;

            return cb.equal(root.get(BookDisplayView_.LANGUAGE_CODE), lang);
        };
    }

    public static Specification<BookDisplayView> hasCategoryId(Integer categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null)
                return null;

            return cb.equal(root.get(BookDisplayView_.CATEGORY_ID), categoryId);
        };
    }

    public static Specification<BookDisplayView> hasAuthorId(Integer authorId) {
        return (root, query, cb) -> {
            if (authorId == null)
                return null;

            return cb.equal(root.join(BookDisplayView_.AUTHORS, JoinType.INNER).get(Author_.ID), authorId);
        };
    }

    public static Specification<BookDisplayView> hasTitleLike(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank())
                return null;

            var lowerTitle = title.toLowerCase();
            return cb.or(
                    cb.like(cb.lower(root.get(BookDisplayView_.TITLE)), "%" + lowerTitle + "%"),
                    cb.greaterThan(cb.function("similarity", Double.class, root.get(BookDisplayView_.TITLE), cb.literal(title)), 0.3)
            );
        };
    }

    public static Specification<BookDisplayView> hasPublishYearBetween(Short minYear, Short maxYear) {
        return (root, query, cb) -> {
            if (minYear == null && maxYear == null)
                return null;

            if (minYear != null && maxYear != null)
                return cb.between(root.get(BookDisplayView_.PUBLISH_YEAR), minYear, maxYear);

            if (minYear != null)
                return cb.greaterThanOrEqualTo(root.get(BookDisplayView_.PUBLISH_YEAR), minYear);

            return cb.lessThanOrEqualTo(root.get(BookDisplayView_.PUBLISH_YEAR), maxYear);
        };
    }

    public static Specification<BookDisplayView> hasLanguageIn(List<String> languages) {
        return (root, query, cb) -> {
            if (languages == null || languages.isEmpty())
                return null;

            return root.get(BookDisplayView_.BOOK_LANGUAGE).in(languages);
        };
    }

    public static Specification<BookDisplayView> hasPagesBetween(Short minPages, Short maxPages) {
        return (root, query, cb) -> {
            if (minPages == null && maxPages == null)
                return null;

            if (minPages != null && maxPages != null)
                return cb.between(root.get(BookDisplayView_.PAGES), minPages, maxPages);

            if (minPages != null)
                return cb.greaterThanOrEqualTo(root.get(BookDisplayView_.PAGES), minPages);

            return cb.lessThanOrEqualTo(root.get(BookDisplayView_.PAGES), maxPages);
        };
    }

}
