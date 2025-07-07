package ru.eventorg.service;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.AllArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyTaskListItemCustom;
import ru.eventorg.dto.UserProfileCustom;
import ru.eventorg.security.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@AllArgsConstructor
public class MyTasksService {
    private final R2dbcEntityTemplate template;

    public Mono<MyTaskListItemCustom> getMyTasksListCustom() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login -> {
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
                            .bind("responsible_user", login)
                            .map(this::mapRowToUserProfileCustom)
                            .one();
                });
    }

    private MyTaskListItemCustom mapRowToUserProfileCustom(Row row, RowMetadata metadata) {
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

}
