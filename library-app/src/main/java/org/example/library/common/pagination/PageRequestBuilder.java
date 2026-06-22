package org.example.library.common.pagination;

import lombok.RequiredArgsConstructor;
import org.example.library.common.exception.InvalidPaginationParameterException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PageRequestBuilder {

    private final SortValidator sortValidator;
    private final PaginationProperties paginationProperties;


    public Pageable buildPageRequest(PaginationParams paginationParams, Set<String> allowedSortFields) {
        int validatedPage = validatePage(paginationParams.getPage());
        int validatedSize = validateSize(paginationParams.getSize());
        sortValidator.validateSortParameters(paginationParams.getSort(), allowedSortFields);

        var sortObj = buildSort(paginationParams.getSort());
        if (!sortObj.isSorted())
            return PageRequest.of(validatedPage, validatedSize);

        return PageRequest.of(validatedPage, validatedSize, sortObj);
    }

    private int validatePage(Integer page) {
        if (page == null || page < 0)
            return 0;

        return page;
    }

    private int validateSize(Integer size) {
        if (size == null) {
            return paginationProperties.getDefaultPageSize();
        }

        if (size < paginationProperties.getMinPageSize()) {
            throw new InvalidPaginationParameterException(
                    "error.pagination.min_page_size", paginationProperties.getMinPageSize()
            );
        }

        if (size > paginationProperties.getMaxPageSize()) {
            throw new InvalidPaginationParameterException(
                    "error.pagination.max_page_size", paginationProperties.getMaxPageSize()
            );
        }

        return size;
    }

    private Sort buildSort(List<String> sort) {
        if (sort == null || sort.isEmpty())
            return Sort.unsorted();

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            String[] parts = sortParam.split(SortValidator.DELIMITER);
            String field = parts[0].trim();

            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length > 1) {
                direction = Sort.Direction.fromString(parts[1].trim());
            }

            orders.add(new Sort.Order(direction, field));
        }

        return Sort.by(orders);
    }

}
