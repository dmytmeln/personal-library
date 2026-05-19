package org.example.library.library_book.dto;

import jakarta.validation.constraints.Size;

public record LocationDto(
        @Size(max = 255, message = "{validation.library_book.location.size}")
        String location
) {
}
