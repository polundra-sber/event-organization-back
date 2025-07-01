package ru.eventorg.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("event_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStatus {
    @Id
    private Integer eventStatusId;
    private String eventStatusName;

}
