package org.example.library.collection_book.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.collection_book.dto.CollectionBookSearchParams;
import org.example.library.collection_book.service.CollectionBookService;
import org.example.library.library_book.dto.BulkRequest;
import org.example.library.library_book.dto.LibraryBookDto;
import org.example.library.pagination.PaginationParams;
import org.example.library.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections/{collectionId}/books")
@RequiredArgsConstructor
public class CollectionBookController {

    private final CollectionBookService service;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<LibraryBookDto> getCollectionBooks(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable int collectionId,
            CollectionBookSearchParams searchParams,
            PaginationParams paginationParams
    ) {
        return service.getCollectionBooksPaginated(userPrincipal.getId(), collectionId, searchParams, paginationParams);
    }

    @PostMapping("/{libraryBookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBookToCollection(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                    @PathVariable int collectionId,
                                    @PathVariable int libraryBookId
    ) {
        service.addBookToCollection(userPrincipal.getId(), collectionId, libraryBookId);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public void bulkAddBooksToCollection(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                         @PathVariable int collectionId,
                                         @Valid @RequestBody BulkRequest request
    ) {
        service.bulkAddBooksToCollection(userPrincipal.getId(), collectionId, request.getIds());
    }

    @DeleteMapping("/{libraryBookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookFromCollection(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                         @PathVariable int collectionId,
                                         @PathVariable int libraryBookId
    ) {
        service.removeBookFromCollection(userPrincipal.getId(), collectionId, libraryBookId);
    }

    @PostMapping("/bulk-remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkRemoveBooksFromCollection(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                              @PathVariable int collectionId,
                                              @Valid @RequestBody BulkRequest request
    ) {
        service.bulkRemoveBooksFromCollection(userPrincipal.getId(), collectionId, request.getIds());
    }

}
