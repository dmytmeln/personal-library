package org.example.library.quote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.quote.domain.Quote;
import org.example.library.quote.dto.QuoteDto;
import org.example.library.quote.dto.QuoteRequest;
import org.example.library.quote.mapper.QuoteMapper;
import org.example.library.quote.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final QuoteRepository repository;
    private final QuoteMapper mapper;
    private final LibraryBookRepository libraryBookRepository;

    @Transactional(readOnly = true)
    public List<QuoteDto> getByLibraryBookId(Integer libraryBookId, Integer userId) {
        return repository.findByLibraryBookIdAndLibraryBookUserIdOrderByCreatedAtDesc(libraryBookId, userId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public QuoteDto create(Integer libraryBookId, QuoteRequest request, Integer userId) {
        var libraryBook = libraryBookRepository.findByIdAndUserId(libraryBookId, userId)
                .orElseThrow(() -> new NotFoundException("error.library_book.not_found"));

        var quote = mapper.toEntity(request);
        quote.setLibraryBook(libraryBook);

        var savedQuote = repository.saveAndFlush(quote);
        log.info("[QUOTE_CREATE] User ID: {}, Library Book ID: {}, Quote ID: {}", userId, libraryBookId, savedQuote.getId());
        return mapper.toDto(savedQuote);
    }

    @Transactional
    public QuoteDto update(Integer quoteId, QuoteRequest request, Integer userId) {
        var quote = repository.findByIdAndLibraryBookUserId(quoteId, userId)
                .orElseThrow(() -> new NotFoundException("error.quote.not_found"));

        mapper.update(quote, request);
        var savedQuote = repository.saveAndFlush(quote);
        log.info("[QUOTE_UPDATE] User ID: {}, Quote ID: {}", userId, quoteId);
        return mapper.toDto(savedQuote);
    }

    @Transactional
    public void delete(Integer quoteId, Integer userId) {
        var quote = repository.findByIdAndLibraryBookUserId(quoteId, userId)
                .orElseThrow(() -> new NotFoundException("error.quote.not_found"));

        repository.delete(quote);
        log.info("[QUOTE_DELETE] User ID: {}, Quote ID: {}", userId, quoteId);
    }

}
