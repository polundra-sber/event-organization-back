package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {
    CREATOR("Создатель"),
    PARTICIPANT("Участник"),
    ORGANIZER("Организатор"),
    NOT_ALLOWED("Не допущен");

    private final String displayName;
}
