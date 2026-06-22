package org.example.library.quote.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.quote.dto.QuoteDto;
import org.example.library.quote.dto.QuoteRequest;
import org.example.library.quote.service.QuoteService;
import org.example.library.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService service;

    @GetMapping
    public List<QuoteDto> getByLibraryBookId(@RequestParam Integer libraryBookId,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.getByLibraryBookId(libraryBookId, userPrincipal.getId());
    }

    @PostMapping("/{libraryBookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteDto create(@PathVariable Integer libraryBookId,
                          @Valid @RequestBody QuoteRequest request,
                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.create(libraryBookId, request, userPrincipal.getId());
    }

    @PutMapping("/{quoteId}")
    public QuoteDto update(@PathVariable Integer quoteId,
                          @Valid @RequestBody QuoteRequest request,
                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.update(quoteId, request, userPrincipal.getId());
    }

    @DeleteMapping("/{quoteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer quoteId,
                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        service.delete(quoteId, userPrincipal.getId());
    }

}
