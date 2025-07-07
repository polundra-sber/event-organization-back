package ru.eventorg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskListResponse {
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;
    private List<TaskListItemCustom> tasks;
}
