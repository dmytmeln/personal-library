package org.example.library.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPaginationParameterException extends RuntimeException {
    private final Object[] args;

    public InvalidPaginationParameterException(String message) {
        super(message);
        this.args = null;
    }

    public InvalidPaginationParameterException(String message, Object... args) {
        super(message);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
