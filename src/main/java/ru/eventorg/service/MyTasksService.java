package ru.eventorg.service;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.AllArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyTaskListItemCustom;
import ru.eventorg.exception.*;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.TaskEntityRepository;
import ru.eventorg.security.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@AllArgsConstructor
public class MyTasksService {
    private final R2dbcEntityTemplate template;
    private final EventEntityRepository eventEntityRepository;
    private final TaskEntityRepository taskRepository;


    public Flux<MyTaskListItemCustom> getMyTasksListCustom() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMapMany(login -> {
                    String query = """
                        SELECT
                            e.event_id,
                            e.event_name,
                            t.task_id,
                            t.task_name,
                            t.deadline_date,
                            t.deadline_time,
                            t.task_description,
                            ts.task_status_name
                        FROM task t
                        JOIN event e ON e.event_id = t.event_id
                        JOIN task_status ts ON ts.task_status_id = t.status_id
                        WHERE t.responsible_user = :login
                        """;

                    return template.getDatabaseClient()
                            .sql(query)
                            .bind("login", login)
                            .map(this::mapRowToTaskItem)
                            .all();
                });
    }

    private MyTaskListItemCustom mapRowToTaskItem(Row row, RowMetadata metadata) {
        return new MyTaskListItemCustom(
                row.get("event_id", Integer.class),
                row.get("event_name", String.class),
                row.get("task_id", Integer.class),
                row.get("task_name", String.class),
                row.get("task_description", String.class),
                row.get("task_status_name", String.class),
                row.get("deadline_date", LocalDate.class),
                row.get("deadline_time", LocalTime.class)
        );
    }


    public Mono<Void> denyTask(Integer taskId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login -> validateTaskExists(taskId)
                        .then(validateTaskResponsibleUser(taskId, login))
                        .then(validateTaskNotCompleted(taskId))  // Новая проверка
                        .then(validateEventIsActiveForTask(taskId))
                        .then(taskRepository.updateResponsibleUser(taskId))
                        .then()
                );
    }


    public Mono<Void> completeTask(Integer taskId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login -> validateTaskExists(taskId)
                        .then(validateTaskResponsibleUser(taskId, login))
                        .then(validateEventIsActiveForTask(taskId))
                        .then(taskRepository.updateTaskStatus(taskId, 1))
                        .then() // Преобразуем Mono<Integer> в Mono<Void>
                );
    }


    private Mono<Void> validateTaskExists(Integer taskId) {
        return taskRepository.existsById(taskId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new TaskNotFoundException(ErrorState.TASK_NOT_EXIST)));
    }


    private Mono<Void> validateTaskResponsibleUser(Integer taskId, String username) {
        return taskRepository.isUserResponsible(taskId, username)
                .flatMap(isResponsible -> isResponsible
                        ? Mono.empty()
                        : Mono.error(new NotResponsibleException(ErrorState.NOT_RESPONSIBLE)));
    }


    private Mono<Void> validateEventIsActiveForTask(Integer taskId) {
        return taskRepository.findEventIdByTaskId(taskId)
                .flatMap(eventEntityRepository::existsActiveEventById)
                .flatMap(isActive -> isActive
                        ? Mono.empty()
                        : Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE)));
    }

    private Mono<Void> validateTaskNotCompleted(Integer taskId) {
        return taskRepository.findTaskStatusById(taskId)
                .flatMap(status -> {
                    if (status == 1) {
                        return Mono.error(new TaskAlreadyCompletedException(ErrorState.TASK_ALREADY_COMPLETED));
                    }
                    return Mono.empty();
                });
    }
}
