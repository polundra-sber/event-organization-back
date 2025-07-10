package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventStatus {
    ACTIVE( "активно"),
    COMPLETED("завершено"),
    DELETED("удалено");

    private final String displayName;
}