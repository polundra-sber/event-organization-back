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
    STUFF_NOT_EXIST(
            HttpStatus.NOT_FOUND,
            "Вещь с указанным идентификатором не найдена"

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
    ),

    TASK_NOT_EXIST(
            HttpStatus.NOT_FOUND,
            "Задача с указанным идентификатором не найдена"

    ),

    CREATOR_CANNOT_LEAVE(
            HttpStatus.FORBIDDEN,
            "Создатель не может покинуть мероприятие"
    ),



    EVENT_NOT_ACTIVE(
            HttpStatus.FORBIDDEN,
            "Мероприятие не активно"
    ),
    PURCHASE_ALREADY_HAS_RESPONSIBLE(
            HttpStatus.FORBIDDEN,
            "Ответственный на покупку уже назначен"
    ),
    STUFF_ALREADY_HAS_RESPONSIBLE(
            HttpStatus.FORBIDDEN,
            "Ответственный на вещь уже назначен"
    );

    private final HttpStatus statusCode;
    private final String message;
}