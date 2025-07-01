package ru.eventorg.entity;


import java.math.BigDecimal;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("purchase")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {
    @Id
    private Integer purchaseId;
    private String purchaseName;
    private String purchaseDescription;
    private BigDecimal cost;
    private String responsibleUser;
    private Integer eventId;

}
