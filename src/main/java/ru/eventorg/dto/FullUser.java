package ru.eventorg.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class FullUser {
    private String login;
    @Nullable
    private String roleName;
    private String email;
    private String password;
    private String name;
    private String surname;
    private String commentMoneyTransfer;
}
