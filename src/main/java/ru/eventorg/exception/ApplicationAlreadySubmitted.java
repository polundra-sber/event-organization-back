package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationAlreadySubmitted extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public ApplicationAlreadySubmitted(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
