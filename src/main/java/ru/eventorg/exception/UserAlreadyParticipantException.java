package ru.eventorg.exception;

public class UserAlreadyParticipantException extends RuntimeException {
    public UserAlreadyParticipantException(String message) {
        super(message);
    }
}
