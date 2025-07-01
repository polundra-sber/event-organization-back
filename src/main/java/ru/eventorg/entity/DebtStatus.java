package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("debt_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtStatus {

    @Id
    private Integer debtStatusId;
    private String debtStatusName;
}
