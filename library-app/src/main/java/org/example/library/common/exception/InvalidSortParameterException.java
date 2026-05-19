package org.example.library.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSortParameterException extends RuntimeException {

    private final Object[] args;

    public InvalidSortParameterException(String message) {
        super(message);
        this.args = null;
    }

    public InvalidSortParameterException(String message, Object... args) {
        super(message);
        this.args = args;
    }

}
