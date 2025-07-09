package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CannotDenyPurchaseException extends RuntimeException {

    private final HttpStatus statusCode;
    private final String message;

    public CannotDenyPurchaseException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
