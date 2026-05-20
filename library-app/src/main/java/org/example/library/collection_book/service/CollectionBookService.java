package org.example.library.collection_book.service;

import lombok.RequiredArgsConstructor;
import org.example.library.collection.repository.CollectionRepository;
import org.example.library.collection_book.domain.CollectionBook;
import org.example.library.collection_book.domain.CollectionBookId;
import org.example.library.collection_book.dto.CollectionBookSearchParams;
import org.example.library.collection_book.repository.CollectionBookRepository;
import org.example.library.common.exception.BadRequestException;
import org.example.library.common.exception.NotFoundException;
import org.example.library.library_book.dto.LibraryBookDto;
import org.example.library.library_book.mapper.LibraryBookMapper;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.library_book.repository.LibraryBookViewRepository;
import org.example.library.common.pagination.PageRequestBuilder;
import org.example.library.common.pagination.PaginationParams;
import org.example.library.common.pagination.SortableFields;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionBookService {

    private final CollectionBookRepository repository;
    private final CollectionRepository collectionRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final LibraryBookViewRepository viewRepository;
    private final LibraryBookMapper libraryBookMapper;
    private final PageRequestBuilder pageRequestBuilder;


    @Transactional(readOnly = true)
    public Page<LibraryBookDto> getCollectionBooksPaginated(Integer userId, Integer collectionId, CollectionBookSearchParams searchParams, PaginationParams paginationParams) {
        if (!collectionRepository.existsByIdAndUserId(collectionId, userId))
            throw new BadRequestException("error.collection.not_belong_to_user");

        var pageable = pageRequestBuilder.buildPageRequest(paginationParams, SortableFields.LIBRARY_BOOK_FIELDS);

        return viewRepository.findCollectionBooks(userId, collectionId, searchParams, pageable)
                .map(libraryBookMapper::toDto);
    }

    @Transactional
    public void addBookToCollection(Integer userId, Integer collectionId, Integer libraryBookId) {
        if (!collectionRepository.existsByIdAndUserId(collectionId, userId))
            throw new BadRequestException("error.collection.not_belong_to_user");

        if (!libraryBookRepository.existsByIdAndUserId(libraryBookId, userId))
            throw new NotFoundException("error.library_book.not_found");

        var id = new CollectionBookId(collectionId, libraryBookId);
        if (repository.existsById(id))
            throw new BadRequestException("error.collection.book_already_added");

        var collectionBook = CollectionBook.builder()
                .id(id)
                .libraryBook(libraryBookRepository.getReferenceById(libraryBookId))
                .collection(collectionRepository.getReferenceById(collectionId))
                .build();

        repository.save(collectionBook);
    }

    @Transactional
    public void bulkAddBooksToCollection(Integer userId, Integer collectionId, List<Integer> libraryBookIds) {
        if (!collectionRepository.existsByIdAndUserId(collectionId, userId))
            throw new BadRequestException("error.collection.not_belong_to_user");

        var libraryBooks = libraryBookRepository.findAllByIdInAndUserId(libraryBookIds, userId);
        if (libraryBooks.isEmpty())
            throw new NotFoundException("error.library_book.none_found");

        var existingInCollection = repository.findLibraryBookIdsByCollectionId(collectionId);

        var newMappings = libraryBooks.stream()
                .filter(lb -> !existingInCollection.contains(lb.getId()))
                .map(lb -> CollectionBook.builder()
                        .id(new CollectionBookId(collectionId, lb.getId()))
                        .libraryBook(lb)
                        .collection(collectionRepository.getReferenceById(collectionId))
                        .build())
                .toList();

        if (!newMappings.isEmpty()) {
            repository.saveAll(newMappings);
        }
    }

    @Transactional
    public void removeBookFromCollection(Integer userId, Integer collectionId, Integer libraryBookId) {
        int deletedCount = repository.deleteByIdAndUserId(collectionId, libraryBookId, userId);
        if (deletedCount == 0) {
            if (!collectionRepository.existsByIdAndUserId(collectionId, userId))
                throw new BadRequestException("error.collection.not_belong_to_user");

            throw new NotFoundException("error.library_book.not_found");
        }
    }

    @Transactional
    public void bulkRemoveBooksFromCollection(Integer userId, Integer collectionId, List<Integer> libraryBookIds) {
        int deletedCount = repository.deleteAllByCollectionIdAndLibraryBookIdInAndUserId(collectionId, libraryBookIds, userId);
        if (deletedCount == 0) {
            if (!collectionRepository.existsByIdAndUserId(collectionId, userId))
                throw new BadRequestException("error.collection.not_belong_to_user");

            throw new NotFoundException("error.collection.books_not_found_in_collection");
        }
    }

    @Transactional
    public void removeBookFromAllCollections(Integer userId, Integer libraryBookId) {
        int deletedCount = repository.deleteByLibraryBookIdAndUserId(libraryBookId, userId);
        if (deletedCount == 0) {
            if (!libraryBookRepository.existsByIdAndUserId(libraryBookId, userId))
                throw new NotFoundException("error.library_book.not_found");

            throw new NotFoundException("error.collection.books_not_found_in_collection");
        }
    }

}
