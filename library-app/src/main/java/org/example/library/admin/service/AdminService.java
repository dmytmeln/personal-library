package org.example.library.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.admin.dto.AdminAuthorDto;
import org.example.library.admin.dto.AdminBookDto;
import org.example.library.admin.dto.AdminCategoryDto;
import org.example.library.author.domain.Author;
import org.example.library.author.domain.AuthorTranslation;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.domain.BookTranslation;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.common.exception.BadRequestException;
import org.example.library.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public AdminBookDto getBook(Integer id) {
        var book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.book.not_found"));

        return AdminBookDto.builder()
                .id(book.getId())
                .categoryId(book.getCategory() != null ? book.getCategory().getId() : null)
                .publishYear(book.getPublishYear())
                .pages(book.getPages())
                .coverImageUrl(book.getCoverImageUrl())
                .authorIds(book.getAuthors().stream().map(Author::getId).toList())
                .translations(book.getTranslations().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> AdminBookDto.AdminBookTranslationDto.builder()
                                .title(e.getValue().getTitle())
                                .bookLanguage(e.getValue().getBookLanguage())
                                .description(e.getValue().getDescription())
                                .build())))
                .build();
    }

    @Transactional(readOnly = true)
    public AdminAuthorDto getAuthor(Integer id) {
        var author = authorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.author.not_found"));

        return AdminAuthorDto.builder()
                .id(author.getId())
                .birthYear(author.getBirthYear())
                .deathYear(author.getDeathYear())
                .translations(author.getTranslations().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> AdminAuthorDto.AdminAuthorTranslationDto.builder()
                                .fullName(e.getValue().getFullName())
                                .country(e.getValue().getCountry())
                                .biography(e.getValue().getBiography())
                                .build())))
                .build();
    }

    @Transactional(readOnly = true)
    public AdminCategoryDto getCategory(Integer id) {
        var category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.category.not_found"));

        return AdminCategoryDto.builder()
                .id(category.getId())
                .translations(category.getTranslations().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> AdminCategoryDto.AdminCategoryTranslationDto.builder()
                                .name(e.getValue().getName())
                                .description(e.getValue().getDescription())
                                .build())))
                .build();
    }

    @Transactional
    public void createBook(AdminBookDto dto) {
        var book = new Book();
        updateBookFields(book, dto);

        var savedBook = bookRepository.save(book);
        log.info("[ADMIN_BOOK_CREATE] Book ID: {}", savedBook.getId());
    }

    @Transactional
    public void updateBook(Integer id, AdminBookDto dto) {
        var book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.book.not_found"));
        updateBookFields(book, dto);

        bookRepository.save(book);
        log.info("[ADMIN_BOOK_UPDATE] Book ID: {}", id);
    }

    @Transactional
    public void deleteBook(Integer id) {
        if (!bookRepository.existsById(id))
            throw new NotFoundException("error.book.not_found");

        bookRepository.deleteById(id);
        log.info("[ADMIN_BOOK_DELETE] Book ID: {}", id);
    }

    @Transactional
    public void deleteBooks(java.util.List<Integer> ids) {
        bookRepository.deleteAllById(ids);
        log.info("[ADMIN_BOOKS_BULK_DELETE] Count: {}", ids.size());
    }

    @Transactional
    public void createAuthor(AdminAuthorDto dto) {
        var author = new Author();
        author.setPopularityCount(0);
        updateAuthorFields(author, dto);

        var savedAuthor = authorRepository.save(author);
        log.info("[ADMIN_AUTHOR_CREATE] Author ID: {}", savedAuthor.getId());
    }

    @Transactional
    public void updateAuthor(Integer id, AdminAuthorDto dto) {
        var author = authorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.author.not_found"));
        updateAuthorFields(author, dto);

        authorRepository.save(author);
        log.info("[ADMIN_AUTHOR_UPDATE] Author ID: {}", id);
    }

    @Transactional
    public void deleteAuthor(Integer id) {
        if (!authorRepository.existsById(id))
            throw new NotFoundException("error.author.not_found");

        if (bookRepository.existsByAuthorsId(id))
            throw new BadRequestException("error.author.has_books");

        authorRepository.deleteById(id);
        log.info("[ADMIN_AUTHOR_DELETE] Author ID: {}", id);
    }

    @Transactional
    public void deleteAuthors(java.util.List<Integer> ids) {
        for (Integer id : ids) {
            if (bookRepository.existsByAuthorsId(id)) {
                throw new BadRequestException("error.author.has_books");
            }
        }

        authorRepository.deleteAllById(ids);
        log.info("[ADMIN_AUTHORS_BULK_DELETE] Count: {}", ids.size());
    }

    @Transactional
    public void createCategory(AdminCategoryDto dto) {
        var category = new Category();
        category.setPopularityCount(0);
        updateCategoryFields(category, dto);

        var savedCategory = categoryRepository.save(category);
        log.info("[ADMIN_CATEGORY_CREATE] Category ID: {}", savedCategory.getId());
    }

    @Transactional
    public void updateCategory(Integer id, AdminCategoryDto dto) {
        var category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.category.not_found"));
        updateCategoryFields(category, dto);

        categoryRepository.save(category);
        log.info("[ADMIN_CATEGORY_UPDATE] Category ID: {}", id);
    }

    @Transactional
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id))
            throw new NotFoundException("error.category.not_found");

        if (bookRepository.existsByCategoryId(id))
            throw new BadRequestException("error.category.has_books");

        categoryRepository.deleteById(id);
        log.info("[ADMIN_CATEGORY_DELETE] Category ID: {}", id);
    }

    @Transactional
    public void deleteCategories(List<Integer> ids) {
        for (Integer id : ids) {
            if (bookRepository.existsByCategoryId(id)) {
                throw new BadRequestException("error.category.has_books");
            }
        }

        categoryRepository.deleteAllById(ids);
        log.info("[ADMIN_CATEGORIES_BULK_DELETE] Count: {}", ids.size());
    }

    private void updateBookFields(Book book, AdminBookDto dto) {
        book.setPublishYear(dto.getPublishYear());
        book.setPages(dto.getPages());
        book.setCoverImageUrl(dto.getCoverImageUrl());
        book.setStatus(BookStatus.PRELIMINARY);
        book.setPopularityCount(0);

        if (dto.getCategoryId() != null) {
            book.setCategory(categoryRepository.getReferenceById(dto.getCategoryId()));
        }

        if (dto.getAuthorIds() != null) {
            var authors = authorRepository.findAllById(dto.getAuthorIds());
            book.setAuthors(new HashSet<>(authors));
        } else if (book.getAuthors() == null) {
            book.setAuthors(new HashSet<>());
        }

        if (dto.getTranslations() != null) {
            if (book.getTranslations() == null) {
                book.setTranslations(new HashMap<>());
            }

            var existingTranslations = book.getTranslations();
            for (var entry : dto.getTranslations().entrySet()) {
                var lang = entry.getKey();
                var transDto = entry.getValue();

                var translation = existingTranslations.get(lang);
                if (translation == null) {
                    translation = BookTranslation.builder()
                            .languageCode(lang)
                            .book(book)
                            .build();
                    existingTranslations.put(lang, translation);
                }

                translation.setBook(book);
                translation.setTitle(transDto.getTitle());
                translation.setBookLanguage(transDto.getBookLanguage());
                translation.setDescription(transDto.getDescription());
            }
        }
    }

    private void updateAuthorFields(Author author, AdminAuthorDto dto) {
        author.setBirthYear(dto.getBirthYear());
        author.setDeathYear(dto.getDeathYear());

        if (dto.getTranslations() != null) {
            if (author.getTranslations() == null) {
                author.setTranslations(new HashMap<>());
            }

            var existingTranslations = author.getTranslations();
            for (var entry : dto.getTranslations().entrySet()) {
                var lang = entry.getKey();
                var transDto = entry.getValue();

                var translation = existingTranslations.get(lang);
                if (translation == null) {
                    translation = AuthorTranslation.builder()
                            .languageCode(lang)
                            .author(author)
                            .build();
                    existingTranslations.put(lang, translation);
                }

                translation.setAuthor(author);
                translation.setFullName(transDto.getFullName());
                translation.setCountry(transDto.getCountry());
                translation.setBiography(transDto.getBiography());
            }
        }
    }

    private void updateCategoryFields(Category category, AdminCategoryDto dto) {
        if (dto.getTranslations() != null) {
            if (category.getTranslations() == null) {
                category.setTranslations(new HashMap<>());
            }

            var existingTranslations = category.getTranslations();
            for (var entry : dto.getTranslations().entrySet()) {
                var lang = entry.getKey();
                var transDto = entry.getValue();

                var translation = existingTranslations.get(lang);
                if (translation == null) {
                    translation = CategoryTranslation.builder()
                            .languageCode(lang)
                            .category(category)
                            .build();
                    existingTranslations.put(lang, translation);
                }

                translation.setCategory(category);
                translation.setName(transDto.getName());
                translation.setDescription(transDto.getDescription());
            }
        }
    }

}
