package ru.eventorg.entity;


import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String login;
    private String name;
    private String surname;
    private String commentMoneyTransfer;
}
