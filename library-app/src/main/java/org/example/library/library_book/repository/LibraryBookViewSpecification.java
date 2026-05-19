package org.example.library.library_book.repository;

import jakarta.persistence.criteria.JoinType;
import org.example.library.author.domain.Author_;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.domain.LibraryBookView;
import org.example.library.library_book.domain.LibraryBookView_;
import org.example.library.library_book.dto.LibraryBookSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class LibraryBookViewSpecification {

    public static Specification<LibraryBookView> fromSearchCriteria(Integer userId, String lang, LibraryBookSearchCriteria criteria) {
        return Specification.where(hasUserId(userId))
                .and(hasLanguageCode(lang))
                .and(hasTitleLike(criteria.getTitle()))
                .and(hasStatus(criteria.getStatus()))
                .and(hasAuthorId(criteria.getAuthorId()))
                .and(hasCategoryId(criteria.getCategoryId()))
                .and(hasPublishYearBetween(criteria.getPublishYearMin(), criteria.getPublishYearMax()))
                .and(hasPagesBetween(criteria.getPagesMin(), criteria.getPagesMax()))
                .and(hasRatingBetween(criteria.getRatingMin(), criteria.getRatingMax()))
                .and(hasLanguageIn(criteria.getLanguages()));
    }

    public static Specification<LibraryBookView> hasUserId(Integer userId) {
        return (root, query, cb) -> {
            if (userId == null)
                return null;

            return cb.equal(root.get(LibraryBookView_.USER_ID), userId);
        };
    }

    public static Specification<LibraryBookView> hasLanguageCode(String lang) {
        return (root, query, cb) -> {
            if (lang == null)
                return null;

            return cb.or(
                    cb.equal(root.get(LibraryBookView_.LANGUAGE_CODE), lang),
                    cb.isNull(root.get(LibraryBookView_.LANGUAGE_CODE))
            );
        };
    }

    public static Specification<LibraryBookView> hasTitleLike(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank())
                return null;

            var lowerTitle = title.toLowerCase();
            return cb.or(
                    cb.like(cb.lower(root.get(LibraryBookView_.TITLE)), "%" + lowerTitle + "%"),
                    cb.greaterThan(cb.function("similarity", Double.class, root.get(LibraryBookView_.TITLE), cb.literal(title)), 0.3)
            );
        };
    }

    public static Specification<LibraryBookView> hasStatus(LibraryBookStatus status) {
        return (root, query, cb) -> {
            if (status == null)
                return null;

            return cb.equal(root.get(LibraryBookView_.STATUS), status);
        };
    }

    public static Specification<LibraryBookView> hasAuthorId(Integer authorId) {
        return (root, query, cb) -> {
            if (authorId == null)
                return null;

            return cb.equal(root.join(LibraryBookView_.AUTHORS, JoinType.INNER).get(Author_.ID), authorId);
        };
    }

    public static Specification<LibraryBookView> hasCategoryId(Integer categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null)
                return null;

            return cb.equal(root.get(LibraryBookView_.CATEGORY_ID), categoryId);
        };
    }

    public static Specification<LibraryBookView> hasPublishYearBetween(Short min, Short max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;

            if (min != null && max != null)
                return cb.between(root.get(LibraryBookView_.PUBLISH_YEAR), min, max);

            if (min != null)
                return cb.greaterThanOrEqualTo(root.get(LibraryBookView_.PUBLISH_YEAR), min);

            return cb.lessThanOrEqualTo(root.get(LibraryBookView_.PUBLISH_YEAR), max);
        };
    }

    public static Specification<LibraryBookView> hasPagesBetween(Short min, Short max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;

            if (min != null && max != null)
                return cb.between(root.get(LibraryBookView_.PAGES), min, max);

            if (min != null)
                return cb.greaterThanOrEqualTo(root.get(LibraryBookView_.PAGES), min);

            return cb.lessThanOrEqualTo(root.get(LibraryBookView_.PAGES), max);
        };
    }

    public static Specification<LibraryBookView> hasRatingBetween(Byte min, Byte max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;

            if (min != null && max != null)
                return cb.between(root.get(LibraryBookView_.RATING), min, max);

            if (min != null)
                return cb.greaterThanOrEqualTo(root.get(LibraryBookView_.RATING), min);

            return cb.lessThanOrEqualTo(root.get(LibraryBookView_.RATING), max);
        };
    }

    public static Specification<LibraryBookView> hasLanguageIn(List<String> languages) {
        return (root, query, cb) -> {
            if (languages == null || languages.isEmpty())
                return null;

            return root.get(LibraryBookView_.BOOK_LANGUAGE).in(languages);
        };
    }

}
