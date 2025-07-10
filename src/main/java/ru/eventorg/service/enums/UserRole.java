package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {
    CREATOR("создатель"),
    PARTICIPANT("участник"),
    ORGANIZER("организатор"),
    NOT_ALLOWED("не допущен");

    private final String displayName;
}
