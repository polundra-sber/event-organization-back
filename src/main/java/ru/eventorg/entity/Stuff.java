package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("stuff")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stuff {
    @Id
    private Integer stuffId;
    private String stuffName;
    private String stuffDescription;
    private Integer eventId;
    private String responsibleUser;

}
