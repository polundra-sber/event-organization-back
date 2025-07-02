package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("payer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payer {
    @Id
    private Integer payerId;
    private Integer purchaseId;
    private String userId;
}
