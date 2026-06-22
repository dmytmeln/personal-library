package org.example.library.library_book.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.library.library_book.domain.LibraryBookStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryBookSearchCriteria {
    private String title;
    private String location;
    private LibraryBookStatus status;
    private Integer authorId;
    private Integer categoryId;
    private Short publishYearMin;
    private Short publishYearMax;
    private Short pagesMin;
    private Short pagesMax;
    private Byte ratingMin;
    private Byte ratingMax;
    private List<String> languages;
}
