package org.example.library.library_book.dto;

import lombok.*;
import org.example.library.book.dto.BookDto;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryBookDto {
    private Integer id;
    private String status;
    private LocalDateTime addedAt;
    private Byte rating;
    private String location;
    private BookDto book;
}