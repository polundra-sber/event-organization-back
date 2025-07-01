package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("user_secrets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSecrets {
    @Id
    private String login;
    private String email;
    private String password;

}
