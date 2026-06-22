package org.example.library.common.exception;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class RestControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ExceptionResponse handleNotFoundException(NotFoundException exception) {
        log.warn("[NOT_FOUND] {}", exception.getMessage());
        return ExceptionResponse.of(tryLocalize(exception.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ExceptionResponse handleBadRequestException(BadRequestException exception) {
        log.warn("[BAD_REQUEST] {}", exception.getMessage());
        return ExceptionResponse.of(tryLocalize(exception.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ExceptionResponse handleValidationException(ValidationException exception) {
        log.warn("[VALIDATION] {}", exception.getMessage());
        return ExceptionResponse.of(tryLocalize(exception.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
                .toList();
        log.warn("[VALIDATION] {}", errors);
        var body = ExceptionResponse.of(errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(UNAUTHORIZED)
    public ExceptionResponse handleBadCredentialsException() {
        log.warn("[BAD_CREDENTIALS] Invalid username or password");
        return ExceptionResponse.of(getMessage("error.auth.invalid_credentials"));
    }

    @ExceptionHandler(InvalidSortParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public ExceptionResponse handleInvalidSortParameterException(InvalidSortParameterException exception) {
        log.warn("[INVALID_SORT] {}", exception.getMessage());
        return ExceptionResponse.of(tryLocalize(exception.getMessage(), exception.getArgs()));
    }

    @ExceptionHandler(InvalidPaginationParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public ExceptionResponse handleInvalidPaginationParameterException(InvalidPaginationParameterException exception) {
        log.warn("[INVALID_PAGINATION] {}", exception.getMessage());
        return ExceptionResponse.of(tryLocalize(exception.getMessage(), exception.getArgs()));
    }

    @ExceptionHandler(MappingException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ExceptionResponse handleMappingException(MappingException exception) {
        log.error("[MAPPING_ERROR] {}", exception.getMessage(), exception);
        return ExceptionResponse.of(getMessage("error.internal_server_error"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ExceptionResponse handleAllExceptions(Exception exception) {
        log.error("[INTERNAL_ERROR] {}", exception.getMessage(), exception);
        return ExceptionResponse.of(getMessage("error.internal_server_error"));
    }

    private String tryLocalize(String message) {
        return tryLocalize(message, null);
    }

    private String tryLocalize(String message, Object[] args) {
        if (message == null) return null;
        try {
            return messageSource.getMessage(message, args, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return message;
        }
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

}
