package org.example.library.library_book.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.author.repository.AuthorRepository;
import org.example.library.book.domain.Book;
import org.example.library.book.domain.BookStatus;
import org.example.library.book.dto.LanguageWithCount;
import org.example.library.book.repository.BookRepository;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.collection_book.repository.CollectionBookRepository;
import org.example.library.common.exception.BadRequestException;
import org.example.library.common.exception.NotFoundException;
import org.example.library.common.pagination.PageRequestBuilder;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.common.pagination.SortableFields;
import org.example.library.library_book.domain.LibraryBook;
import org.example.library.library_book.domain.LibraryBookStatus;
import org.example.library.library_book.domain.LibraryBookView;
import org.example.library.library_book.dto.*;
import org.example.library.library_book.mapper.LibraryBookMapper;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.library_book.repository.LibraryBookViewRepository;
import org.example.library.library_book.repository.LibraryBookViewSpecification;
import org.example.library.recommendation.adapter.EmbeddingModelAdapter;
import org.example.library.recommendation.event.UserProfileUpdatedEvent;
import org.example.library.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LibraryBookService {

    private static final int RATING_LOWER_BOUND = 0;
    private static final int RATING_UPPER_BOUND = 5;


    private final LibraryBookRepository repository;
    private final LibraryBookViewRepository viewRepository;
    private final CollectionBookRepository collectionBookRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final LibraryBookMapper mapper;
    private final PageRequestBuilder pageRequestBuilder;
    private final ApplicationEventPublisher eventPublisher;
    private final EmbeddingModelAdapter embeddingModelAdapter;


    @Transactional(readOnly = true)
    public Page<LibraryBookDto> getAllByUserId(Integer userId, LibraryBookSearchCriteria criteria, PaginationParams paginationParams) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        var spec = LibraryBookViewSpecification.fromSearchCriteria(userId, lang, criteria);
        var pageable = pageRequestBuilder.buildPageRequest(paginationParams, SortableFields.LIBRARY_BOOK_FIELDS);

        return viewRepository.findAll(spec, pageable)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<LanguageWithCount> getLanguagesByUserId(Integer userId) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return repository.findLanguagesWithCountByUserId(userId, lang);
    }

    @Transactional
    public void createLocalBook(CreateLocalBookDto dto, Integer userId) {
        var book = new Book();
        book.setOwner(userRepository.getReferenceById(userId));
        book.setStatus(BookStatus.NEW);
        book.setPopularityCount(0);
        boolean hasCategory = dto.getCategoryId() != null;
        if (hasCategory) {
            book.setCategory(categoryRepository.getReferenceById(dto.getCategoryId()));
        }
        boolean hasAuthors = dto.getAuthorIds() != null && !dto.getAuthorIds().isEmpty();
        if (hasAuthors) {
            book.setAuthors(new HashSet<>(authorRepository.findAllById(dto.getAuthorIds())));
        }

        var savedBook = bookRepository.save(book);

        var libraryBook = LibraryBook.of(savedBook, userRepository.getReferenceById(userId));
        libraryBook.setStatus(dto.getStatus());
        libraryBook.setTitle(dto.getTitle());
        libraryBook.setDescription(dto.getDescription());
        libraryBook.setPublishYear(dto.getPublishYear());
        libraryBook.setPages(dto.getPages());
        libraryBook.setLanguage(dto.getBookLanguage());
        libraryBook.setCustomCategoryName(dto.getCustomCategoryName());
        libraryBook.setCustomAuthorName(dto.getCustomAuthorName());

        repository.save(libraryBook);
        log.info("[LIBRARY_BOOK_LOCAL_CREATE] User ID: {}, Book ID: {}", userId, savedBook.getId());
    }

    @Transactional
    public void create(Integer bookId, Integer userId) {
        if (repository.existsByBookIdAndUserId(bookId, userId))
            throw new BadRequestException("error.library_book.already_added");

        var book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("error.book.not_found"));

        if (book.getOwner() != null && !book.getOwner().getId().equals(userId))
            throw new BadRequestException("error.library_book.access_denied");

        repository.save(LibraryBook.of(book, userRepository.getReferenceById(userId)));

        if (book.getOwner() == null) {
            incrementPopularity(List.of(bookId));
            eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));
        }

        log.info("[LIBRARY_BOOK_ADD] User ID: {}, Book ID: {}", userId, bookId);
    }

    @Transactional
    public void bulkAdd(List<Integer> bookIds, Integer userId) {
        var existingIds = repository.findExistingBookIdsInLibrary(userId, bookIds);

        var newBookIds = bookIds.stream()
                .filter(id -> !existingIds.contains(id))
                .distinct()
                .toList();
        if (newBookIds.isEmpty()) return;

        var books = bookRepository.findAllById(newBookIds);
        if (books.isEmpty())
            throw new NotFoundException("error.book.none_found");

        var accessibleBooks = books.stream()
                .filter(b -> b.getOwner() == null || b.getOwner().getId().equals(userId))
                .toList();
        if (accessibleBooks.isEmpty()) return;

        var libraryBooks = accessibleBooks.stream()
                .map(book -> LibraryBook.of(book, userRepository.getReferenceById(userId)))
                .toList();
        repository.saveAll(libraryBooks);

        var globalBookIds = accessibleBooks.stream()
                .filter(b -> b.getOwner() == null)
                .map(Book::getId)
                .toList();
        if (!globalBookIds.isEmpty()) {
            incrementPopularity(globalBookIds);
            eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));
        }

        log.info("[LIBRARY_BOOK_BULK_ADD] User ID: {}, Accessible Book IDs: {}", userId, accessibleBooks.stream().map(Book::getId).toList());
    }

    @Transactional
    public LibraryBookDto rate(Integer libraryBookId, Integer userId, Integer rating) {
        if (rating < RATING_LOWER_BOUND || rating > RATING_UPPER_BOUND)
            throw new BadRequestException("error.library_book.invalid_rating");

        int updatedCount = repository.updateRating(libraryBookId, userId, rating.byteValue());
        if (updatedCount == 0)
            throw new NotFoundException("error.library_book.not_found");

        repository.flush();
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));

        var view = getViewById(libraryBookId);
        log.info("[LIBRARY_BOOK_RATE] User ID: {}, Library Book ID: {}, Rating: {}", userId, libraryBookId, rating);

        return mapper.toDto(view);
    }

    @Transactional
    public LibraryBookDto updateStatus(Integer libraryBookId, Integer userId, LibraryBookStatus status) {
        var libraryBook = getExistingById(libraryBookId, userId);
        updateBookStatus(libraryBook, status);
        repository.saveAndFlush(libraryBook);

        eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));

        var view = getViewById(libraryBookId);
        log.info("[LIBRARY_BOOK_STATUS_UPDATE] User ID: {}, Library Book ID: {}, Status: {}", userId, libraryBookId, status);

        return mapper.toDto(view);
    }

    @Transactional
    public void bulkUpdateStatus(List<Integer> libraryBookIds, Integer userId, LibraryBookStatus status) {
        var libraryBooks = repository.findAllByIdInAndUserId(libraryBookIds, userId);
        if (libraryBooks.isEmpty()) return;

        libraryBooks.forEach(lb -> updateBookStatus(lb, status));
        repository.saveAll(libraryBooks);

        eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));
        log.info("[LIBRARY_BOOK_BULK_STATUS_UPDATE] User ID: {}, Library Book IDs: {}, Status: {}", userId, libraryBookIds, status);
    }

    @Transactional
    public LibraryBookDto updateLocalBook(Integer libraryBookId, UpdateLocalBookDto dto, Integer userId) {
        var libraryBook = repository.findByIdAndUserIdWithBook(libraryBookId, userId)
                .orElseThrow(() -> new NotFoundException("error.library_book.not_found"));

        var book = libraryBook.getBook();

        if (book.getOwner() == null || !book.getOwner().getId().equals(userId))
            throw new BadRequestException("error.library_book.access_denied");

        book.setPublishYear(dto.getPublishYear());
        book.setPages(dto.getPages());

        if (dto.getCategoryId() != null) {
            book.setCategory(categoryRepository.getReferenceById(dto.getCategoryId()));
        } else {
            book.setCategory(null);
        }

        if (dto.getAuthorIds() != null && !dto.getAuthorIds().isEmpty()) {
            book.setAuthors(new HashSet<>(authorRepository.findAllById(dto.getAuthorIds())));
        } else {
            book.getAuthors().clear();
        }

        bookRepository.save(book);

        libraryBook.setTitle(dto.getTitle());
        libraryBook.setDescription(dto.getDescription());
        libraryBook.setPublishYear(dto.getPublishYear());
        libraryBook.setPages(dto.getPages());
        libraryBook.setLanguage(dto.getBookLanguage());
        libraryBook.setCustomCategoryName(dto.getCustomCategoryName());
        libraryBook.setCustomAuthorName(dto.getCustomAuthorName());

        repository.saveAndFlush(libraryBook);
        var updatedView = getViewById(libraryBookId);
        log.info("[LIBRARY_BOOK_LOCAL_UPDATE] User ID: {}, Library Book ID: {}", userId, libraryBookId);
        return mapper.toDto(updatedView);
    }

    @Transactional
    public LibraryBookDto updateLocation(Integer libraryBookId, Integer userId, LocationDto dto) {
        var libraryBook = getExistingById(libraryBookId, userId);
        libraryBook.setLocation(dto.location());
        repository.saveAndFlush(libraryBook);
        var updatedView = getViewById(libraryBookId);
        log.info("[LIBRARY_BOOK_LOCATION_UPDATE] User ID: {}, Library Book ID: {}, Location: {}", userId, libraryBookId, dto.location());
        return mapper.toDto(updatedView);
    }

    @Transactional
    public LibraryBookDto updateDetails(Integer libraryBookId, Integer userId, UpdateLibraryBookDetailsDto dto) {
        var libraryBook = getExistingById(libraryBookId, userId);
        mapper.update(libraryBook, dto);
        repository.saveAndFlush(libraryBook);
        var updatedView = getViewById(libraryBookId);
        log.info("[LIBRARY_BOOK_DETAILS_UPDATE] User ID: {}, Library Book ID: {}", userId, libraryBookId);
        return mapper.toDto(updatedView);
    }

    @Transactional
    public LibraryBookDto resetDetails(Integer libraryBookId, Integer userId) {
        int updatedCount = repository.resetOverriddenFields(libraryBookId, userId);
        if (updatedCount == 0)
            throw new NotFoundException("error.library_book.not_found");

        repository.flush();
        var updatedView = getViewById(libraryBookId);
        log.info("[LIBRARY_BOOK_DETAILS_RESET] User ID: {}, Library Book ID: {}", userId, libraryBookId);
        return mapper.toDto(updatedView);
    }

    @Transactional
    public void delete(Integer libraryBookId, Integer userId) {
        var libraryBook = repository.findByIdAndUserIdWithBook(libraryBookId, userId)
                .orElseThrow(() -> new NotFoundException("error.library_book.not_found"));
        var book = libraryBook.getBook();
        collectionBookRepository.deleteByLibraryBookIdAndUserId(libraryBookId, userId);
        repository.delete(libraryBook);

        if (book.getOwner() == null) {
            decrementPopularity(List.of(book.getId()));
            eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));
        }
        log.info("[LIBRARY_BOOK_DELETE] User ID: {}, Library Book ID: {}", userId, libraryBookId);
    }

    @Transactional
    public void bulkDelete(List<Integer> libraryBookIds, Integer userId) {
        var libraryBooks = repository.findAllByIdInAndUserIdWithBook(libraryBookIds, userId);
        if (libraryBooks.isEmpty()) return;

        var globalBookIds = libraryBooks.stream()
                .filter(lb -> lb.getBook().getOwner() == null)
                .map(lb -> lb.getBook().getId())
                .toList();

        collectionBookRepository.deleteAllByLibraryBookIdInAndUserId(libraryBookIds, userId);
        repository.deleteAll(libraryBooks);

        if (!globalBookIds.isEmpty()) {
            decrementPopularity(globalBookIds);
            eventPublisher.publishEvent(new UserProfileUpdatedEvent(userId));
        }
        log.info("[LIBRARY_BOOK_BULK_DELETE] User ID: {}, Library Book IDs: {}, Status: SUCCESS", userId, libraryBookIds);
    }

    @Transactional(readOnly = true)
    public List<LibraryBookDto> searchByMood(String query, LibraryBookStatus status, Integer userId, Integer limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        float[] queryVector = embeddingModelAdapter.embed(query);
        var lang = LocaleContextHolder.getLocale().getLanguage();
        int validatedLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        var statusStr = status != null ? status.name() : null;

        var results = viewRepository.searchByMood(queryVector, lang, userId, statusStr, validatedLimit);

        return results.stream()
                .map(mapper::toDto)
                .toList();
    }

    private void updateBookStatus(LibraryBook libraryBook, LibraryBookStatus status) {
        var oldStatus = libraryBook.getStatus();

        if (status == LibraryBookStatus.READ && oldStatus != LibraryBookStatus.READ) {
            libraryBook.setFinishedAt(LocalDate.now());
        } else if (status != LibraryBookStatus.READ && oldStatus == LibraryBookStatus.READ) {
            libraryBook.setFinishedAt(null);
        }

        libraryBook.setStatus(status);
    }

    private void incrementPopularity(List<Integer> bookIds) {
        bookRepository.incrementPopularityCount(bookIds);
        categoryRepository.incrementPopularityCountByBookIds(bookIds);
        authorRepository.incrementPopularityCountByBookIds(bookIds);
    }

    private void decrementPopularity(List<Integer> bookIds) {
        bookRepository.decrementPopularityCount(bookIds);
        categoryRepository.decrementPopularityCountByBookIds(bookIds);
        authorRepository.decrementPopularityCountByBookIds(bookIds);
    }

    private LibraryBook getExistingById(Integer libraryBookId, Integer userId) {
        return repository.findByIdAndUserId(libraryBookId, userId)
                .orElseThrow(() -> new NotFoundException("error.library_book.not_found"));
    }

    private LibraryBookView getViewById(Integer libraryBookId) {
        var lang = LocaleContextHolder.getLocale().getLanguage();
        return viewRepository.findByIdAndLanguageCode(libraryBookId, lang)
                .orElseThrow(() -> new NotFoundException("error.library_book.view_not_found"));
    }

}
