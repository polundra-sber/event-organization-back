package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebtStatus {
    NOT_PAID("не оплачено"),
    PAID("оплачено"),
    RECEIVED("получено");

    private final String displayName;
}
