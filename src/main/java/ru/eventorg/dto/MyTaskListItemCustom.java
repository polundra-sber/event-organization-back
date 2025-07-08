package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyTaskListItemCustom {
    private Integer eventId;
    private String eventName;
    private Integer taskId;
    private String taskName;
    private String taskDescription;
    private String statusName;
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;
}
