package ru.eventorg.entity;


import java.time.LocalDate;
import java.time.LocalTime;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Table("task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    private Integer taskId;
    private String taskName;
    private String taskDescription;
    private Integer statusId;
    private Integer eventId;
    private String responsibleUser;
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;

}
