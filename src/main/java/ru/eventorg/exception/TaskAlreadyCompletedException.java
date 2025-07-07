package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TaskAlreadyCompletedException extends RuntimeException {
    private final HttpStatus statusCode;
    private final String message;

    public TaskAlreadyCompletedException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
