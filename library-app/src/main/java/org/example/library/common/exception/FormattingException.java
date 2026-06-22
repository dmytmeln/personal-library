package org.example.library.common.exception;

public class FormattingException extends RuntimeException {

    public FormattingException(String message) {
        super(message);
    }

    public FormattingException(String message, Throwable cause) {
        super(message, cause);
    }

}
