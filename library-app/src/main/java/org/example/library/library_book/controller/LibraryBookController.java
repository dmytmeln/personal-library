package org.example.library.library_book.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.book.dto.LanguageWithCount;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.dto.*;
import org.example.library.library_book.service.LibraryBookService;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/library-books")
@RequiredArgsConstructor
public class LibraryBookController {

    private final LibraryBookService service;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<LibraryBookDto> getAll(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       PaginationParams paginationParams,
                                       LibraryBookSearchCriteria criteria
    ) {
        return service.getAllByUserId(userPrincipal.getId(), criteria, paginationParams);
    }

    @GetMapping("/languages")
    @ResponseStatus(HttpStatus.OK)
    public List<LanguageWithCount> getLanguages(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.getLanguagesByUserId(userPrincipal.getId());
    }

    @GetMapping("/search-by-mood")
    @ResponseStatus(HttpStatus.OK)
    public List<LibraryBookDto> searchByMood(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                             @RequestParam String query,
                                             @RequestParam(required = false) LibraryBookStatus status,
                                             @RequestParam(required = false) Integer limit) {
        return service.searchByMood(query, status, userPrincipal.getId(), limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam Integer bookId) {
        service.create(bookId, userPrincipal.getId());
    }

    @PostMapping("/local")
    @ResponseStatus(HttpStatus.CREATED)
    public void createLocalBook(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody CreateLocalBookDto dto) {
        service.createLocalBook(dto, userPrincipal.getId());
    }

    @PutMapping("/local/{libraryBookId}")
    @ResponseStatus(HttpStatus.OK)
    public LibraryBookDto updateLocalBook(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                          @PathVariable Integer libraryBookId,
                                          @Valid @RequestBody org.example.library.library_book.dto.UpdateLocalBookDto dto
    ) {
        return service.updateLocalBook(libraryBookId, dto, userPrincipal.getId());
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public void bulkAdd(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody BulkRequest request) {
        service.bulkAdd(request.getIds(), userPrincipal.getId());
    }

    @PutMapping("/{libraryBookId}/rating")
    @ResponseStatus(HttpStatus.OK)
    public LibraryBookDto rate(@AuthenticationPrincipal UserPrincipal userPrincipal,
                               @PathVariable Integer libraryBookId,
                               @RequestParam Integer rating
    ) {
        return service.rate(libraryBookId, userPrincipal.getId(), rating);
    }

    @PutMapping("/{libraryBookId}/status")
    @ResponseStatus(HttpStatus.OK)
    public LibraryBookDto updateStatus(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       @PathVariable Integer libraryBookId,
                                       @RequestParam LibraryBookStatus status
    ) {
        return service.updateStatus(libraryBookId, userPrincipal.getId(), status);
    }

    @PutMapping("/{libraryBookId}/location")
    @ResponseStatus(HttpStatus.OK)
    public LibraryBookDto updateLocation(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                         @PathVariable Integer libraryBookId,
                                         @Valid @RequestBody LocationDto dto
    ) {
        return service.updateLocation(libraryBookId, userPrincipal.getId(), dto);
    }

    @PutMapping("/bulk-status")
    @ResponseStatus(HttpStatus.OK)
    public void bulkUpdateStatus(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody BulkStatusUpdateRequest request) {
        service.bulkUpdateStatus(request.getIds(), userPrincipal.getId(), request.getStatus());
    }

    @PutMapping("/{libraryBookId}/details")
    @ResponseStatus(HttpStatus.OK)
    public LibraryBookDto updateDetails(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                        @PathVariable Integer libraryBookId,
                                        @Valid @RequestBody UpdateLibraryBookDetailsDto dto
    ) {
        return service.updateDetails(libraryBookId, userPrincipal.getId(), dto);
    }

    @PutMapping("/{libraryBookId}/details/reset")
    @ResponseStatus(HttpStatus.OK)
    public LibraryBookDto resetDetails(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       @PathVariable Integer libraryBookId
    ) {
        return service.resetDetails(libraryBookId, userPrincipal.getId());
    }

    @DeleteMapping("/{libraryBookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Integer libraryBookId) {
        service.delete(libraryBookId, userPrincipal.getId());
    }

    @PostMapping("/bulk-remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkDelete(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody BulkRequest request) {
        service.bulkDelete(request.getIds(), userPrincipal.getId());
    }

}
