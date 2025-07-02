package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FullUser {
    private String login;
    private String email;
    private String password;
    private String name;
    private String surname;
    private String commentMoneyTransfer;
}
