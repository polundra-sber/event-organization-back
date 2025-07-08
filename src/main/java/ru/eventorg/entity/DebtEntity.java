package ru.eventorg.entity;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("debt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtEntity {
    @Id
    private Integer debtId;
    private String payerId;
    private String recipientId;
    private Integer statusId;
    private Float debtAmount;
    private Integer eventId;
}
