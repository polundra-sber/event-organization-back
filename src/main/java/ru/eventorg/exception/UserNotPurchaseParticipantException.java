package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserNotPurchaseParticipantException extends RuntimeException {

    private final HttpStatus statusCode;
    private final String message;

    public UserNotPurchaseParticipantException(ErrorState error) {
        this.statusCode = error.getStatusCode();
        this.message = error.getMessage();
    }
}
