package ru.eventorg.repository;

public interface PurchaseWithResponsibleUserProjection {
    // Поля из таблицы purchase
    Integer getPurchaseId();
    String getPurchaseName();
    String getPurchaseDescription();
    String getResponsibleUser();
    Integer getEventId();

    // Поля из таблицы user_profile
    String getUserLogin();
    String getUserName();
    String getUserSurname();
    String getCommentMoneyTransfer();
}