package org.example.library.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.example.library.book.dto.BookDto;
import org.example.library.book.mapper.BookMapper;
import org.example.library.book.repository.BookDisplayViewRepository;
import org.example.library.book.repository.BookRepository;
import org.example.library.common.exception.InvalidPaginationParameterException;
import org.example.library.common.exception.NotFoundException;
import org.example.library.common.pagination.PaginationProperties;
import org.example.library.recommendation.adapter.EmbeddingModelAdapter;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserProfileService userProfileService;
    private final BookDisplayViewRepository bookDisplayViewRepository;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final PaginationProperties paginationProperties;
    private final EmbeddingModelAdapter embeddingModelAdapter;


    @Transactional
    public List<BookDto> getRecommendations(Integer userId, Integer limit) {
        var userProfileEmbeddingOpt = userProfileService.getUserProfileEmbedding(userId);

        if (userProfileEmbeddingOpt.isEmpty()) {
            return Collections.emptyList();
        }

        int validatedLimit = validateLimit(limit);
        var languageCode = LocaleContextHolder.getLocale().getLanguage();
        float[] userProfileEmbedding = userProfileEmbeddingOpt.get();

        var similarBooks = bookDisplayViewRepository.findSimilarBooks(userProfileEmbedding, languageCode, userId, validatedLimit);

        return bookMapper.toBookDtos(similarBooks);
    }

    @Transactional(readOnly = true)
    public List<BookDto> searchByMood(String query, Integer userId, Integer limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        float[] queryVector = embeddingModelAdapter.embed(query);
        int validatedLimit = validateLimit(limit);
        var languageCode = LocaleContextHolder.getLocale().getLanguage();

        var books = bookDisplayViewRepository.findSimilarBooks(queryVector, languageCode, userId, validatedLimit);

        return bookMapper.toBookDtos(books);
    }

    @Transactional(readOnly = true)
    public List<BookDto> getSimilarBooks(Integer bookId, Integer userId, Integer limit) {
        var book = bookRepository.findEmbeddingById(bookId)
                .orElseThrow(() -> new NotFoundException("error.book.not_found"));

        float[] bookEmbedding = book.getEmbedding();
        int validatedLimit = validateLimit(limit);
        var languageCode = LocaleContextHolder.getLocale().getLanguage();

        var similarBooks = bookDisplayViewRepository.findSimilarBooksExcluding(bookEmbedding, languageCode, userId, bookId, validatedLimit);

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
