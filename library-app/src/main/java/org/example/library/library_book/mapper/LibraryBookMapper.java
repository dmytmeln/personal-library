package org.example.library.library_book.mapper;

import org.example.library.author.domain.Author;
import org.example.library.book.dto.BookDto;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookView;
import org.example.library.library_book.dto.LibraryBookDto;
import org.example.library.library_book.dto.UpdateLibraryBookDetailsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Mapper(componentModel = "spring")
public interface LibraryBookMapper {

    @Mapping(target = "book", source = "libraryBookView")
    LibraryBookDto toDto(LibraryBookView libraryBookView);

    @Mapping(target = "id", source = "bookId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "categoryName", source = "view", qualifiedByName = "getLocalizedCategoryNameFromView")
    @Mapping(target = "publishYear", source = "publishYear")
    @Mapping(target = "language", source = "bookLanguage")
    @Mapping(target = "pages", source = "pages")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "coverImageUrl", source = "coverImageUrl")
    @Mapping(target = "authors", source = "authors", qualifiedByName = "authorsToMap")
    @Mapping(target = "customAuthorName", source = "customAuthorName")
    @Mapping(target = "ownerId", source = "ownerUserId")
    BookDto toBookDto(LibraryBookView view);

    @Named("getLocalizedCategoryNameFromView")
    default String getLocalizedCategoryNameFromView(LibraryBookView view) {
        if (view.getCategoryName() != null && !view.getCategoryName().isBlank())
            return view.getCategoryName();

        var lang = LocaleContextHolder.getLocale().getLanguage();
        var translation = view.getCategory().getTranslations().get(lang);
        return translation != null ? translation.getName() : null;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "customAuthorName", ignore = true)
    @Mapping(target = "customCategoryName", ignore = true)
    @Mapping(target = "finishedAt", ignore = true)
    @Mapping(target = "location", ignore = true)
    void update(@MappingTarget LibraryBook libraryBook, UpdateLibraryBookDetailsDto dto);

    @Named("authorsToMap")
    default Map<Integer, String> authorsToMap(Collection<Author> authors) {
        if (authors == null)
            return null;

        var lang = LocaleContextHolder.getLocale().getLanguage();
        return authors.stream()
                .collect(toMap(Author::getId, a -> {
                    var translation = a.getTranslations().get(lang);
                    if (translation == null)
                        throw new IllegalStateException("Translation not found for author: " + a.getId());

                    return translation.getFullName();
                }));
    }

}
