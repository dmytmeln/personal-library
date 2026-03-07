package org.example.library.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.admin.dto.AdminAuthorDto;
import org.example.library.admin.dto.AdminBookDto;
import org.example.library.admin.dto.AdminCategoryDto;
import org.example.library.admin.service.AdminService;
import org.example.library.library_book.dto.BulkRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // Books
    @GetMapping("/books/{id}")
    public AdminBookDto getBook(@PathVariable Integer id) {
        return adminService.getBook(id);
    }

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public void createBook(@RequestBody AdminBookDto dto) {
        adminService.createBook(dto);
    }

    @PutMapping("/books/{id}")
    public void updateBook(@PathVariable Integer id, @RequestBody AdminBookDto dto) {
        adminService.updateBook(id, dto);
    }

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Integer id) {
        adminService.deleteBook(id);
    }

    @PostMapping("/books/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooks(@RequestBody BulkRequest request) {
        adminService.deleteBooks(request.getIds());
    }

    // Authors
    @GetMapping("/authors/{id}")
    public AdminAuthorDto getAuthor(@PathVariable Integer id) {
        return adminService.getAuthor(id);
    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public void createAuthor(@RequestBody AdminAuthorDto dto) {
        adminService.createAuthor(dto);
    }

    @PutMapping("/authors/{id}")
    public void updateAuthor(@PathVariable Integer id, @RequestBody AdminAuthorDto dto) {
        adminService.updateAuthor(id, dto);
    }

    @DeleteMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable Integer id) {
        adminService.deleteAuthor(id);
    }

    @PostMapping("/authors/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthors(@RequestBody BulkRequest request) {
        adminService.deleteAuthors(request.getIds());
    }

    // Categories
    @GetMapping("/categories/{id}")
    public AdminCategoryDto getCategory(@PathVariable Integer id) {
        return adminService.getCategory(id);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCategory(@RequestBody AdminCategoryDto dto) {
        adminService.createCategory(dto);
    }

    @PutMapping("/categories/{id}")
    public void updateCategory(@PathVariable Integer id, @RequestBody AdminCategoryDto dto) {
        adminService.updateCategory(id, dto);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Integer id) {
        adminService.deleteCategory(id);
    }

    @PostMapping("/categories/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategories(@RequestBody BulkRequest request) {
        adminService.deleteCategories(request.getIds());
    }

}
