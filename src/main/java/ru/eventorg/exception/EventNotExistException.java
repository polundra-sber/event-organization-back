package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class EventNotExistException extends RuntimeException {

    private final HttpStatus statusCode;
    private final String message;

    public EventNotExistException() {
        this.statusCode = HttpStatus.NOT_FOUND;
        this.message = "Мероприятие с данным идентификатором не найдено";
    }

    @Override
    public String getMessage() {
        return message;
    }
}