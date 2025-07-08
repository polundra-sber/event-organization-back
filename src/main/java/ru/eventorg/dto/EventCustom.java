package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class EventCustom {
    private Integer eventId;
    private String eventName;
    private String eventDescription;
    private String status;
    private String location;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String chatLink;
    private String role;
}
