package ru.eventorg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EventNotExistException extends ResponseStatusException {
    public EventNotExistException() {
        super(HttpStatus.NOT_FOUND,"Мероприятие с данным идентификатором не найдено");
    }
}
