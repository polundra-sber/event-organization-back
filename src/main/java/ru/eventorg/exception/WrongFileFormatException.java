package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WrongFileFormatException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public WrongFileFormatException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
