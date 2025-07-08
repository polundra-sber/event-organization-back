package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserNotRecipientException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public UserNotRecipientException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
