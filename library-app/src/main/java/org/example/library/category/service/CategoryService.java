package org.example.library.category.service;

import lombok.RequiredArgsConstructor;
import org.example.library.category.dto.CategoryDto;
import org.example.library.category.dto.CategorySearchParams;
import org.example.library.category.dto.CategoryWithBooksCount;
import org.example.library.category.mapper.CategoryMapper;
import org.example.library.category.repository.CategoryDisplayViewRepository;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.common.exception.NotFoundException;
import org.example.library.common.pagination.PageRequestBuilder;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.common.pagination.SortableFields;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryDisplayViewRepository displayViewRepository;
    private final CategoryMapper mapper;
    private final PageRequestBuilder pageRequestBuilder;


    public Page<CategoryWithBooksCount> search(PaginationParams paginationParams, CategorySearchParams searchParams) {
        var pageable = pageRequestBuilder.buildPageRequest(paginationParams, SortableFields.CATEGORY_FIELDS);
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.searchWithBooksCount(
                searchParams.getName(),
                searchParams.getBooksCountMin(),
                searchParams.getBooksCountMax(),
                lang,
                pageable);
    }

    public CategoryDto getById(Integer categoryId) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return displayViewRepository.findByIdAndLanguageCode(categoryId, lang)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("error.category.not_found"));
    }

    public Page<CategoryWithBooksCount> searchForUser(Integer userId, PaginationParams paginationParams, CategorySearchParams searchParams) {
        var pageable = pageRequestBuilder.buildPageRequest(paginationParams, SortableFields.CATEGORY_FIELDS);
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.searchForUser(
                userId,
                searchParams.getName(),
                searchParams.getBooksCountMin(),
                searchParams.getBooksCountMax(),
                lang,
                pageable);
    }

}
