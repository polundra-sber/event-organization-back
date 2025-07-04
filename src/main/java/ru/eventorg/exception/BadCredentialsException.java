package ru.eventorg.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadCredentialsException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public BadCredentialsException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}