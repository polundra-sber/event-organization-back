package ru.eventorg.entity;


import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {
    @Id
    private Integer eventId;
    private String eventName;
    private String eventDescription;
    private Integer statusId;
    private String location;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String chatLink;
    private Boolean costAllocated;
}
