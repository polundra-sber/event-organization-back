package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AlreadyHasResponsibleException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public AlreadyHasResponsibleException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
