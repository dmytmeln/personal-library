package org.example.library.library_book.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.example.library.library_book.domain.LibraryBookStatus;

import java.util.List;

@Data
@Builder
public class CreateLocalBookDto {

    @NotBlank
    private String title;

    private String description;

    private String bookLanguage;

    @NotNull
    private LibraryBookStatus status;

    private Short publishYear;
    private Short pages;

    private Integer categoryId;
    private List<Integer> authorIds;

    private String customCategoryName;
    private String customAuthorName;

    @AssertTrue(message = "{validation.library_book.category_not_specified}")
    public boolean isCategorySpecified() {
        return categoryId != null || (customCategoryName != null && !customCategoryName.isBlank());
    }

    @AssertTrue(message = "{validation.library_book.author_not_specified}")
    public boolean isAuthorSpecified() {
        return (authorIds != null && !authorIds.isEmpty()) || (customAuthorName != null && !customAuthorName.isBlank());
    }

}
