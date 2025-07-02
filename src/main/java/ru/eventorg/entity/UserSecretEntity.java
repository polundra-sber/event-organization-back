package ru.eventorg.entity;


import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("user_secret")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSecretEntity {
    private String login;
    private String email;
    private String password;

}
