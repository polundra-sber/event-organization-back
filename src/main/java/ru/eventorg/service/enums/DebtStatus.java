package ru.eventorg.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebtStatus {
    NOT_PAID("не оплачен"),
    PAID("оплачен"),
    RECEIVED("получен");

    private final String displayName;
}
