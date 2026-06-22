package org.example.library.book.service;

import lombok.RequiredArgsConstructor;
import org.example.library.book.dto.BookDetails;
import org.example.library.book.mapper.BookMapper;
import org.example.library.book.repository.BookDisplayViewRepository;
import org.example.library.collection.service.CollectionService;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.mapper.LibraryBookMapper;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.library_book.repository.LibraryBookViewRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookDetailsService {

    private final CollectionService collectionService;
    private final LibraryBookRepository libraryBookRepository;
    private final LibraryBookViewRepository libraryBookViewRepository;
    private final BookDisplayViewRepository bookDisplayViewRepository;
    private final BookMapper bookMapper;
    private final LibraryBookMapper libraryBookMapper;

    @Transactional(readOnly = true)
    public BookDetails getDetails(Integer bookId, Integer userId) {
        var lang = LocaleContextHolder.getLocale().getLanguage();

        var libraryBookViewOpt = libraryBookViewRepository.findByBookIdAndUserIdAndLanguageCode(bookId, userId, lang);
        var collections = collectionService.getAllByUserIdAndBookId(userId, bookId);
        var ratingSummary = libraryBookRepository.findAverageRatingAndCountByBookId(bookId);

        var builder = BookDetails.builder()
                .collections(collections)
                .averageRating(Optional.ofNullable(ratingSummary.getAverageRating()).orElse(0.0))
                .ratingsNumber(Optional.ofNullable(ratingSummary.getRatingsCount()).orElse(0L));

        if (libraryBookViewOpt.isPresent()) {
            builder.libraryBook(libraryBookMapper.toDto(libraryBookViewOpt.get()));
        } else {
            var bookView = bookDisplayViewRepository.findByIdAndLanguageCode(bookId, lang)
                    .orElseThrow(() -> new NotFoundException("error.book.not_found"));
            builder.book(bookMapper.toBookDto(bookView));
        }

        return builder.build();
    }

}
