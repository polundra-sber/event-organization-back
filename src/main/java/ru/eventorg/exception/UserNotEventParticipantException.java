package ru.eventorg.exception;

public class UserNotEventParticipantException extends RuntimeException {
    public UserNotEventParticipantException() {
        super("Пользователь не является участником мероприятия");
    }
}
