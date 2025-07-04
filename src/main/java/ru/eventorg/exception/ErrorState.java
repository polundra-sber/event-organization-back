package ru.eventorg.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorState {
    BAD_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "Пользователь указал неверный логин или пароль"
    ),
    EVENT_NOT_EXIST(
            HttpStatus.NOT_FOUND,
            "Мероприятие с данным идентификатором не найдено"
    ),
    USER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Пользователь уже зарегистрирован"
    ),
    USER_ALREADY_JOINED(
            HttpStatus.CONFLICT,
            "Вы уже присоединены к данному мероприятию"
    ),
    USER_NOT_EVENT_PARTICIPANT(
            HttpStatus.FORBIDDEN,
            "Пользователь не является участником мероприятия"
    );

    private final HttpStatus statusCode;
    private final String message;
}