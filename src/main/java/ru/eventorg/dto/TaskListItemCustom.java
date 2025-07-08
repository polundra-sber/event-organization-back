package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskListItemCustom {
    private Integer taskId;
    private String taskName;
    private String taskDescription;
    private String statusName;
    private Integer eventId;
    private String responsibleUserLogin;
    private String responsibleUserName;
    private String responsibleUserSurname;
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;

}
