package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileCustom {
    private String login;
    private String email;
    private String name;
    private String surname;
    private String commentMoneyTransfer;
}
