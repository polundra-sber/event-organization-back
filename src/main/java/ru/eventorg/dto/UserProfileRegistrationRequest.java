package ru.eventorg.dto;

public record UserProfileRegistrationRequest(
        String login,
        String name,
        String surname,
        String email,
        String password,
        String commentMoneyTransfer
) {}