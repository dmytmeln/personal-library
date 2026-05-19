package org.example.library.library_book.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.example.library.collection_book.dto.CollectionBookSearchParams;
import org.example.library.library_book.domain.LibraryBookView;
import org.example.library.library_book.domain.LibraryBookView_;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LibraryBookViewRepositoryCustomImpl implements LibraryBookViewRepositoryCustom {

    private static final Map<String, String> SORT_MAPPING = new HashMap<>();

    static {
        SORT_MAPPING.put(LibraryBookView_.ID, "library_book_id");
        SORT_MAPPING.put(LibraryBookView_.ADDED_AT, "added_at");
        SORT_MAPPING.put(LibraryBookView_.PUBLISH_YEAR, "publish_year");
    }


    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public Page<LibraryBookView> findCollectionBooks(Integer userId, Integer collectionId, CollectionBookSearchParams searchParams, Pageable pageable) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        var whereSql = buildWhereQueryPart(searchParams);
        var totalCount = getLibraryBooksCount(userId, collectionId, searchParams, whereSql, lang);
        var results = getLibraryBooks(userId, collectionId, searchParams, pageable, whereSql, lang);
        return new PageImpl<>(results, pageable, totalCount);
    }

    private List<LibraryBookView> getLibraryBooks(Integer userId, Integer collectionId, CollectionBookSearchParams searchParams, Pageable pageable, StringBuilder whereSql, String lang) {
        var dataSql = buildLibraryBookQuery(pageable, whereSql);

        Query query = entityManager.createNativeQuery(dataSql.toString(), LibraryBookView.class);
        bindParameters(query, userId, collectionId, searchParams, lang);
        query.setParameter("limit", pageable.getPageSize());
        query.setParameter("offset", pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<LibraryBookView> results = query.getResultList();
        return results;
    }

    private StringBuilder buildLibraryBookQuery(Pageable pageable, StringBuilder whereSql) {
        var dataSql = new StringBuilder("SELECT DISTINCT lbv.* " + whereSql);
        if (pageable.getSort().isSorted()) {
            dataSql.append("ORDER BY ");
            pageable.getSort().forEach(order -> {
                String column = SORT_MAPPING.getOrDefault(order.getProperty(), order.getProperty());
                dataSql.append("lbv.").append(column).append(" ").append(order.getDirection().name()).append(", ");
            });
            dataSql.setLength(dataSql.length() - 2);
        } else {
            dataSql.append(" ORDER BY lbv.added_at DESC ");
        }

        dataSql.append(" LIMIT :limit OFFSET :offset");
        return dataSql;
    }

    private long getLibraryBooksCount(Integer userId, Integer collectionId, CollectionBookSearchParams searchParams, StringBuilder whereSql, String lang) {
        var countSql = "SELECT count(DISTINCT lbv.library_book_id) " + whereSql;
        var countQuery = entityManager.createNativeQuery(countSql);
        bindParameters(countQuery, userId, collectionId, searchParams, lang);
        return ((Number) countQuery.getSingleResult()).longValue();
    }

    private StringBuilder buildWhereQueryPart(CollectionBookSearchParams searchParams) {
        var baseSql = """
                FROM library_books_view lbv
                JOIN collection_books cb ON lbv.library_book_id = cb.library_book_id
                WHERE lbv.user_id = :userId AND (lbv.language_code = :lang OR lbv.language_code IS NULL)
                """;

        var whereSql = new StringBuilder(baseSql);
        if (searchParams.isRecursive()) {
            whereSql.append("AND cb.collection_id IN (SELECT * FROM get_collection_subtree_ids(:collectionId)) ");
        } else {
            whereSql.append("AND cb.collection_id = :collectionId ");
        }

        if (searchParams.getTitle() != null && !searchParams.getTitle().isBlank()) {
            whereSql.append("AND (lbv.title ILIKE :title OR lbv.title % :rawTitle) ");
        }

        return whereSql;
    }

    private void bindParameters(Query query, Integer userId, Integer collectionId, CollectionBookSearchParams searchParams, String lang) {
        query.setParameter("userId", userId);
        query.setParameter("collectionId", collectionId);
        query.setParameter("lang", lang);
        if (searchParams.getTitle() != null && !searchParams.getTitle().isBlank()) {
            query.setParameter("title", "%" + searchParams.getTitle() + "%");
            query.setParameter("rawTitle", searchParams.getTitle());
        }
    }

}
