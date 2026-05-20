package org.example.library.common.pagination;

import java.util.Set;

public class SortableFields {

    private static final String BOOKS_COUNT_FIELD = "booksCount";
    private static final String POPULARITY_COUNT_FIELD = "popularityCount";
    private static final String FULL_NAME_FIELD = "fullName";
    private static final String NAME_FIELD = "name";
    private static final String TITLE_FIELD = "title";
    private static final String PUBLISH_YEAR_FIELD = "publishYear";
    private static final String PAGES_FIELD = "pages";
    private static final String LANGUAGE_FIELD = "bookLanguage";
    private static final String CATEGORY_ID_FIELD = "categoryId";
    private static final String CATEGORY_NAME_FIELD = "categoryName";
    private static final String ADDED_AT_FIELD = "addedAt";
    private static final String RATING_FIELD = "rating";
    private static final String STATUS_FIELD = "status";
    private static final String ID_FIELD = "id";
    private static final String BIRTH_YEAR_FIELD = "birthYear";
    private static final String DEATH_YEAR_FIELD = "deathYear";
    private static final String COUNTRY_FIELD = "country";

    private SortableFields() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static final Set<String> BOOK_FIELDS = Set.of(
            ID_FIELD, TITLE_FIELD, PUBLISH_YEAR_FIELD, LANGUAGE_FIELD, PAGES_FIELD,
            CATEGORY_ID_FIELD, CATEGORY_NAME_FIELD, POPULARITY_COUNT_FIELD
    );

    public static final Set<String> AUTHOR_FIELDS = Set.of(
            ID_FIELD, FULL_NAME_FIELD, COUNTRY_FIELD, BIRTH_YEAR_FIELD, DEATH_YEAR_FIELD, 
            BOOKS_COUNT_FIELD, POPULARITY_COUNT_FIELD
    );

    public static final Set<String> CATEGORY_FIELDS = Set.of(
            ID_FIELD, NAME_FIELD, BOOKS_COUNT_FIELD, POPULARITY_COUNT_FIELD
    );

    public static final Set<String> LIBRARY_BOOK_FIELDS = Set.of(
            ID_FIELD, STATUS_FIELD, ADDED_AT_FIELD, RATING_FIELD, 
            TITLE_FIELD, PUBLISH_YEAR_FIELD, PAGES_FIELD, LANGUAGE_FIELD, CATEGORY_NAME_FIELD
    );

}
