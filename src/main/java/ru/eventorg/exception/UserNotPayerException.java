package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserNotPayerException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public UserNotPayerException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
