package org.example.library.quote.dto;

import java.time.LocalDateTime;

public record QuoteDto(
    Integer id,
    Integer libraryBookId,
    String text,
    String page,
    String comment,
    LocalDateTime createdAt
) {}
