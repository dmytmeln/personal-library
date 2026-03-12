package org.example.library.library_category.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.category.dto.CategorySearchParams;
import org.example.library.category.dto.CategoryWithBooksCount;
import org.example.library.category.service.CategoryService;
import org.example.library.pagination.PaginationParams;
import org.example.library.security.UserDetailsImpl;
import org.example.library.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/library-categories")
@RequiredArgsConstructor
public class LibraryCategoryController {

    private final CategoryService service;

    @GetMapping
    public Page<CategoryWithBooksCount> getAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            PaginationParams paginationParams,
            CategorySearchParams searchParams
    ) {
        return service.searchForUser(userPrincipal.getId(), paginationParams, searchParams);
    }

}
