package org.example.library.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExceptionResponse(String message, List<Map<String, String>> errors) {
    public static ExceptionResponse of(String message) {
        return new ExceptionResponse(message, null);
    }

    public static ExceptionResponse of(List<Map<String, String>> errors) {
        return new ExceptionResponse(null, errors);
    }
}
