package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatus {
    COMPLETED("выполнена"),
    NOT_COMPLETED("не выполнена");

    private final String displayName;
}
