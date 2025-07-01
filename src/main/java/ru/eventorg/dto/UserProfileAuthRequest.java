package ru.eventorg.dto;

public record UserProfileAuthRequest(
        String login,
        String password
) {}
