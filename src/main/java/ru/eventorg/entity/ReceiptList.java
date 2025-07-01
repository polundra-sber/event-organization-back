package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("receipt_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptList {
    @Id
    private Integer receiptListId;
    private Integer purchaseId;
    private Integer receiptId;

}
