package ru.eventorg.exception;

public class PurchaseNotExistException extends RuntimeException {
    public PurchaseNotExistException() {
        super("Покупка с указанным идентификатором не найдена");
    }
}