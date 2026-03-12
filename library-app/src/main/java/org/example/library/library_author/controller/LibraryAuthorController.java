package org.example.library.library_author.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.author.dto.AuthorSearchParams;
import org.example.library.author.dto.AuthorWithBooksCount;
import org.example.library.author.dto.CountryWithCount;
import org.example.library.author.service.AuthorService;
import org.example.library.pagination.PaginationParams;
import org.example.library.security.UserDetailsImpl;
import org.example.library.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/library-authors")
@RequiredArgsConstructor
public class LibraryAuthorController {

    private final AuthorService service;

    @GetMapping
    public Page<AuthorWithBooksCount> getAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            PaginationParams paginationParams,
            AuthorSearchParams searchParams
    ) {
        return service.searchForUser(userPrincipal.getId(), paginationParams, searchParams);
    }

    @GetMapping("/countries")
    public List<CountryWithCount> getCountries(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return service.getCountriesForUser(userPrincipal.getId());
    }

}
