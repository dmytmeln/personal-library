package org.example.library.quote.dto;

import jakarta.validation.constraints.NotBlank;

public record QuoteRequest(
    @NotBlank(message = "Quote text cannot be blank")
    String text,
    String page,
    String comment
) {}
