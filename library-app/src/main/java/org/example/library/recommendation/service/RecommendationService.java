package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.example.library.book.dto.BookDto;
import org.example.library.book.mapper.BookMapper;
import org.example.library.book.repository.BookDisplayViewRepository;
import org.example.library.book.repository.BookRepository;
import org.example.library.exception.InvalidPaginationParameterException;
import org.example.library.exception.NotFoundException;
import org.example.library.pagination.PaginationProperties;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserProfileService userProfileService;
    private final BookDisplayViewRepository bookDisplayViewRepository;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final PaginationProperties paginationProperties;


    @Transactional
    public List<BookDto> getRecommendations(Integer userId, Integer limit) {
        float[] userVector = userProfileService.calculateUserProfileVector(userId);

        if (userVector == null) {
            return Collections.emptyList();
        }
        int validatedLimit = validateLimit(limit);

        var languageCode = LocaleContextHolder.getLocale().getLanguage();
        var similarBooks = bookDisplayViewRepository.findSimilarBooks(
                userVector, languageCode, userId, validatedLimit);

        return bookMapper.toBookDtos(similarBooks);
    }

    @Transactional(readOnly = true)
    public List<BookDto> getSimilarBooks(Integer bookId, Integer userId, Integer limit) {
        var book = bookRepository.findDescriptionVectorById(bookId)
                .orElseThrow(() -> new NotFoundException("error.book.not_found"));
        float[] bookVector = book.getDescriptionVector();

        if (bookVector == null) {
            return Collections.emptyList();
        }
        int validatedLimit = validateLimit(limit);

        var languageCode = LocaleContextHolder.getLocale().getLanguage();
        var similarBooks = bookDisplayViewRepository.findSimilarBooksExcluding(
                bookVector, languageCode, userId, bookId, validatedLimit);

        return bookMapper.toBookDtos(similarBooks);
    }

    @Transactional(readOnly = true)
    public List<BookDto> getPopularBooks(Integer userId, Integer limit) {
        int validatedLimit = validateLimit(limit);
        var since = LocalDateTime.now().minusMonths(1);
        var languageCode = LocaleContextHolder.getLocale().getLanguage();
        var books = bookDisplayViewRepository.findPopularBooksRecently(languageCode, userId, since, validatedLimit);

        return bookMapper.toBookDtos(books);
    }

    @Transactional(readOnly = true)
    public List<BookDto> getNewArrivals(Integer userId, Integer limit) {
        int validatedLimit = validateLimit(limit);
        short currentYear = (short) Year.now().getValue();
        var languageCode = LocaleContextHolder.getLocale().getLanguage();
        var books = bookDisplayViewRepository.findNewArrivals(languageCode, userId, currentYear, validatedLimit);

        return bookMapper.toBookDtos(books);
    }

    @Transactional(readOnly = true)
    public List<BookDto> getTrendingInFavoriteGenres(Integer userId, Integer limit) {
        int validatedLimit = validateLimit(limit);
        var languageCode = LocaleContextHolder.getLocale().getLanguage();
        var books = bookDisplayViewRepository.findTrendingInFavoriteGenres(languageCode, userId, validatedLimit);

        return bookMapper.toBookDtos(books);
    }

    private int validateLimit(Integer limit) {
        if (limit == null)
            return paginationProperties.getDefaultPageSize();

        if (limit < paginationProperties.getMinPageSize()) {
            throw new InvalidPaginationParameterException(
                    "error.pagination.min_page_size", paginationProperties.getMinPageSize());
        }
        if (limit > paginationProperties.getMaxPageSize()) {
            throw new InvalidPaginationParameterException(
                    "error.pagination.max_page_size", paginationProperties.getMaxPageSize());
        }

        return limit;
    }

}
