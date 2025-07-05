package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventStatus {
    ACTIVE( "Активно"),
    COMPLETED("Завершено"),
    DELETED("Удалено");

    private final String displayName;
}