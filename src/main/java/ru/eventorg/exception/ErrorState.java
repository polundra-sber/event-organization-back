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
    ),
    PURCHASE_NOT_EXIST(
            HttpStatus.NOT_FOUND,
            "Покупка с указанным идентификатором не найдена"

    ),
    NOT_CREATOR_ROLE(
            HttpStatus.FORBIDDEN,
            "Вы не являетесь создателем мероприятия"
    ),

    NOT_CREATOR_OR_ORGANIZER_ROLE(
            HttpStatus.FORBIDDEN,
            "Вы не являетесь организатором или создателем мероприятия"
    ),
    STUB(
            HttpStatus.I_AM_A_TEAPOT,
            "ЭТО ЗАГЛУШКА ПОКА НЕТ НОРМАЛЬНОГО СТАТУСА"
    );

    private final HttpStatus statusCode;
    private final String message;
}