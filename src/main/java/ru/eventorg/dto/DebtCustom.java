package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtCustom {
    private Integer eventId;
    private String eventName;
    private Integer debtId;
    private String recipientLogin;
    private String recipientName;
    private String recipientSurname;
    private String commentMoneyTransfer;
    private String debtStatusName;
    private Float debtAmount;
}
