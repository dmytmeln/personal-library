
package org.example.library.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.collection.domain.Collection;
import org.example.library.collection.dto.*;
import org.example.library.collection.mapper.CollectionMapper;
import org.example.library.collection.repository.CollectionRepository;
import org.example.library.collection.repository.CollectionSpecification;
import org.example.library.collection_book.domain.CollectionBook;
import org.example.library.collection_book.domain.CollectionBookId;
import org.example.library.collection_book.repository.CollectionBookRepository;
import org.example.library.exception.BadRequestException;
import org.example.library.exception.NotFoundException;
import org.example.library.library_book.repository.LibraryBookRepository;
import org.example.library.user.repository.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private static final int MAX_ALLOWED_DEPTH = 4;


    private final CollectionRepository collectionRepository;
    private final CollectionBookRepository collectionBookRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final UserRepository userRepository;
    private final CollectionMapper collectionMapper;


    @Transactional(readOnly = true)
    public List<BasicCollectionDto> getAllCollections(Integer userId, Integer libraryBookId) {
        Specification<Collection> spec = CollectionSpecification.withUserIdAndOptionalLibraryBookId(userId, libraryBookId);
        return collectionMapper.toBasicDto(collectionRepository.findAll(spec));
    }

    @Transactional(readOnly = true)
    public List<BasicCollectionDto> getAllByUserIdAndBookId(Integer userId, Integer bookId) {
        return collectionMapper.toBasicDto(collectionRepository.findAllByUserIdAndBookId(userId, bookId));
    }

    @Transactional(readOnly = true)
    public List<CollectionNodeDto> getUserCollectionTree(Integer userId) {
        var allCollections = collectionRepository.findAllByUserId(userId);
        var collectionsLookupMap = allCollections.stream()
                .collect(Collectors.toMap(Collection::getId, collectionMapper::toNodeDto));

        var rootNodes = new ArrayList<CollectionNodeDto>();
        for (var collection : allCollections) {
            var currentDto = collectionsLookupMap.get(collection.getId());
            if (collection.getParent() == null) {
                rootNodes.add(currentDto);
            } else {
                var parentDto = collectionsLookupMap.get(collection.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(currentDto);
                }
            }
        }
        return rootNodes;
    }

    @Transactional(readOnly = true)
    public CollectionDetailsDto getCollectionDetails(Integer collectionId, Integer userId) {
        var collection = collectionRepository.findByIdAndUserIdWithChildren(collectionId, userId)
                .orElseThrow(() -> new NotFoundException("error.collection.not_found"));

        var detailsDto = collectionMapper.toDetailsDto(collection);
        detailsDto.setAncestors(collectionRepository.findAncestors(collectionId).stream()
                .map(collectionMapper::toBasicDto)
                .toList());

        return detailsDto;
    }

    @Transactional
    public BasicCollectionDto createCollection(CreateCollectionRequest dto, Integer userId) {
        var newCollection = collectionMapper.toEntity(dto);
        newCollection.setUser(userRepository.getReferenceById(userId));

        if (dto.getParentId() != null) {
            if (!collectionRepository.existsByIdAndUserId(dto.getParentId(), userId))
                throw new NotFoundException("error.collection.parent_not_found");

            int parentDepth = collectionRepository.getDepth(dto.getParentId());
            if (parentDepth >= MAX_ALLOWED_DEPTH)
                throw new BadRequestException("error.collection.max_depth_exceeded");

            newCollection.setParent(collectionRepository.getReferenceById(dto.getParentId()));
        }

        var savedCollection = collectionRepository.save(newCollection);
        log.info("[COLLECTION_CREATE] User ID: {}, Collection ID: {}", userId, savedCollection.getId());
        return collectionMapper.toBasicDto(savedCollection);
    }

    @Transactional
    public BasicCollectionDto updateCollection(Integer collectionId, UpdateCollectionDto dto, Integer userId) {
        var collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new NotFoundException("error.collection.not_found"));

        collectionMapper.updateFromDto(dto, collection);
        var savedCollection = collectionRepository.save(collection);
        log.info("[COLLECTION_UPDATE] User ID: {}, Collection ID: {}", userId, collectionId);
        return collectionMapper.toBasicDto(savedCollection);
    }

    @Transactional
    public void moveCollection(Integer collectionId, Integer newParentId, Integer userId) {
        // todo divide into move and makeRoot operations
        if (Objects.equals(collectionId, newParentId))
            throw new BadRequestException("error.collection.cannot_be_own_parent");

        var collectionToMove = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new NotFoundException("error.collection.not_found"));

        if (newParentId != null) {
            var newParent = collectionRepository.findByIdAndUserIdWithChildren(newParentId, userId)
                    .orElseThrow(() -> new NotFoundException("error.collection.not_found"));
            validateMove(collectionToMove, newParent);
            newParent.addChildrenCollection(collectionToMove);
        } else {
            collectionToMove.setParent(null);
        }

        collectionRepository.save(collectionToMove);
        log.info("[COLLECTION_MOVE] User ID: {}, Collection ID: {}, New Parent ID: {}", userId, collectionId, newParentId);
    }

    @Transactional
    public void deleteCollection(Integer collectionId, Integer userId) {
        int deletedCount = collectionRepository.deleteById(collectionId, userId);
        if (deletedCount == 0)
            throw new NotFoundException("error.collection.not_found");
        log.info("[COLLECTION_DELETE] User ID: {}, Collection ID: {}", userId, collectionId);
    }

    @Transactional
    public void moveBook(Integer sourceCollectionId, Integer targetCollectionId, Integer libraryBookId, Integer userId) {
        if (sourceCollectionId.equals(targetCollectionId))
            return;

        if (!libraryBookRepository.existsByIdAndUserId(libraryBookId, userId))
            throw new NotFoundException("error.library_book.not_found");

        if (collectionRepository.countByUserIdAndIds(userId, sourceCollectionId, targetCollectionId) != 2)
            throw new NotFoundException("error.collection.not_found");

        int deletedCount = collectionBookRepository.deleteByLibraryBookIdAndCollectionId(libraryBookId, sourceCollectionId);
        if (deletedCount == 0)
            throw new NotFoundException("error.collection.book_not_in_source");

        var targetId = new CollectionBookId(targetCollectionId, libraryBookId);
        if (collectionBookRepository.existsById(targetId))
            throw new BadRequestException("error.collection.book_already_in_target");

        var libraryBookRef = libraryBookRepository.getReferenceById(libraryBookId);
        var collectionRef = collectionRepository.getReferenceById(targetCollectionId);
        var newMapping = CollectionBook.builder()
                .id(targetId)
                .libraryBook(libraryBookRef)
                .collection(collectionRef)
                .build();
        collectionBookRepository.save(newMapping);
        log.info("[COLLECTION_BOOK_MOVE] User ID: {}, Library Book ID: {}, Source Collection ID: {}, Target Collection ID: {}", userId, libraryBookId, sourceCollectionId, targetCollectionId);
    }

    private void validateMove(Collection toMove, Collection newParent) {
        if (newParent == null)
            return;

        var validation = collectionRepository.getValidationData(
                toMove.getId(),
                newParent.getId());

        if (validation.getIsCircular())
            throw new BadRequestException("error.collection.circular_dependency");

        if (validation.getParentRootDepth() + validation.getSubtreeDepth() > MAX_ALLOWED_DEPTH)
            throw new BadRequestException("error.collection.max_depth_exceeded");
    }

}
