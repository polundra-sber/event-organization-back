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

    USER_NOT_PURCHASE_PARTICIPANT(
            HttpStatus.FORBIDDEN,
            "Пользователь не является участником покупки"
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

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Пользователь с таким email уже зарегистрирован"
    ),

    EVENT_NOT_ACTIVE(
            HttpStatus.FORBIDDEN,
            "Мероприятие не активно"
    ),

    ROLE_IS_UNCHANGEABLE(
            HttpStatus.CONFLICT,
            "Нельзя поменять роль создателя"
    ),


    PURCHASE_ALREADY_HAS_RESPONSIBLE(
            HttpStatus.FORBIDDEN,
            "Ответственный на покупку уже назначен"
    ),

    PURCHASE_DOES_NOT_HAVE_RESPONSIBLE(
            HttpStatus.CONFLICT,
            "Ответственный на покупку не назначен"
    ),

    NOT_RESPONSIBLE(
            HttpStatus.FORBIDDEN,
            "Вы не являетесь ответственным"
    ),

    TASK_ALREADY_COMPLETED(
            HttpStatus.FORBIDDEN,
            "Нельзя отказаться от выполненной задачи"
    ),

    NOT_RECIPIENT(
            HttpStatus.FORBIDDEN,
            "Вы не являетесь получателем средств"
    ),

    NOT_PAYER(
            HttpStatus.FORBIDDEN,
            "Вы не являетесь плательщиком этого долга"
    ),

    DEBT_NOT_EXIST(
            HttpStatus.NOT_FOUND,
            "Долг с указанным идентификатором не найден"
    ),
    APPLICATION_SUBMITTED(
            HttpStatus.CONFLICT,
            "Заявка на присоединение к мероприятию в обработке"
    ),

    STUFF_ALREADY_HAS_RESPONSIBLE(
            HttpStatus.FORBIDDEN,
            "Ответственный на вещь уже назначен"
    ),

    CANNOT_DENY_PURCHASE(
            HttpStatus.FORBIDDEN,
            "Покупке уже добавлена стоимость"
    );

    private final HttpStatus statusCode;
    private final String message;
}