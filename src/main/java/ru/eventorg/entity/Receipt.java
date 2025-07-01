package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {
    @Id
    private Integer receiptId;
    private String filePath;

}
