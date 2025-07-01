package ru.eventorg.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfiles {
    private String login;
    private String name;
    private String surname;
    private String commentMoneyTransfer;
}
