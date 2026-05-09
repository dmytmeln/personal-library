package org.example.library.pagination;

import org.example.library.exception.InvalidSortParameterException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SortValidator {

    public static final String DELIMITER = ";";
    private static final Set<String> ALLOWED_DIRECTIONS = Set.of("ASC", "DESC");


    public void validateSortParameters(List<String> sort, Set<String> allowedFields) {
        if (sort == null || sort.isEmpty())
            return;

        for (String sortParam : sort) {
            var parts = sortParam.split(DELIMITER);
            var field = parts[0].trim();

            if (parts.length > 2) {
                throw new InvalidSortParameterException(
                        "error.pagination.invalid_sort_format", sortParam
                );
            }

            if (!allowedFields.contains(field)) {
                throw new InvalidSortParameterException(
                        "error.pagination.invalid_sort_field", field, allowedFields
                );
            }

            if (parts.length > 1) {
                var direction = parts[1].trim().toUpperCase();
                if (!ALLOWED_DIRECTIONS.contains(direction)) {
                    throw new InvalidSortParameterException(
                            "error.pagination.invalid_sort_direction", direction, ALLOWED_DIRECTIONS
                    );
                }
            }
        }
    }

}
