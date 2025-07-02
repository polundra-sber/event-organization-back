package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    private Integer roleId;
    private String roleName;

}
