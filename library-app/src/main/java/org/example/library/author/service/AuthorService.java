package org.example.library.author.service;

import lombok.RequiredArgsConstructor;
import org.example.library.author.dto.AuthorDto;
import org.example.library.author.dto.AuthorSearchParams;
import org.example.library.author.dto.AuthorWithBooksCount;
import org.example.library.author.dto.CountryWithCount;
import org.example.library.author.mapper.AuthorMapper;
import org.example.library.author.repository.AuthorDisplayViewRepository;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.exception.NotFoundException;
import org.example.library.pagination.PageRequestBuilder;
import org.example.library.pagination.PaginationParams;
import org.example.library.pagination.SortableFields;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository repository;
    private final AuthorDisplayViewRepository displayViewRepository;
    private final AuthorMapper mapper;
    private final PageRequestBuilder pageRequestBuilder;


    public Page<AuthorWithBooksCount> search(PaginationParams paginationParams, AuthorSearchParams searchParams) {
        var pageable = pageRequestBuilder.buildPageRequest(paginationParams, SortableFields.AUTHOR_FIELDS);
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.searchWithBooksCount(
                searchParams.getName(),
                searchParams.getCountry(),
                searchParams.getBirthYearMin(),
                searchParams.getBirthYearMax(),
                searchParams.getBooksCountMin(),
                searchParams.getBooksCountMax(),
                lang,
                pageable);
    }

    public AuthorDto getById(Integer authorId) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return displayViewRepository.findByIdAndLanguageCode(authorId, lang)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("error.author.not_found"));
    }

    public List<CountryWithCount> getAllCountries() {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.findAllCountriesWithCount(lang);
    }

    public Page<AuthorWithBooksCount> searchForUser(Integer userId, PaginationParams paginationParams, AuthorSearchParams searchParams) {
        var pageable = pageRequestBuilder.buildPageRequest(paginationParams, SortableFields.AUTHOR_FIELDS);
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.searchForUser(
                userId,
                searchParams.getName(),
                searchParams.getCountry(),
                searchParams.getBirthYearMin(),
                searchParams.getBirthYearMax(),
                searchParams.getBooksCountMin(),
                searchParams.getBooksCountMax(),
                lang,
                pageable);
    }

    public List<CountryWithCount> getCountriesForUser(Integer userId) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.findAllCountriesForUser(userId, lang);
    }

}
