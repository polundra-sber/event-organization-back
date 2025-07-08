package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtIncomesCustom {
    private Integer eventId;
    private String eventName;
    private Integer debtId;
    private String payerLogin;
    private String payerName;
    private String payerSurname;
    private String debtStatusName;
    private Float debtAmount;
}
