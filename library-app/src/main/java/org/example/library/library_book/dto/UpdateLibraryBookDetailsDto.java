package org.example.library.library_book.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.example.library.common.validation.AtLeastOneNotNull;

@AtLeastOneNotNull(fieldNames = {"title", "publishYear", "pages", "language", "description"})
@Builder
public record UpdateLibraryBookDetailsDto(
        @Size(min = 1, max = 500, message = "{validation.library_book.title.size}") String title,
        @Positive(message = "{validation.library_book.year.positive}") Short publishYear,
        @Positive(message = "{validation.library_book.pages.positive}") Short pages,
        @Size(max = 50, message = "{validation.library_book.language.size}") String language,
        @Size(max = 2000, message = "{validation.library_book.description.size}") String description
) {
}
