package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.repository.RoleEntityRepository;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final R2dbcEntityTemplate template;

    private final String sqlCheckRole = """
            SELECT role.role_name
            FROM event_user_list eul
            JOIN role ON eul.role_id = role.role_id
            WHERE eul.event_id = :eventId AND eul.user_id = :login
            """;

    public Mono<Boolean> checkIfCreator(Integer eventId, String login) {
        return template.getDatabaseClient()
                .sql(sqlCheckRole)
                .bind("eventId", eventId)
                .bind("login", login)
                .map(row -> row.get("role_name", String.class))
                .first()
                .map(roleName -> "Создатель".equalsIgnoreCase(roleName))
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> checkIfOrganizerOrHigher(Integer eventId, String login) {
        return template.getDatabaseClient()
                .sql(sqlCheckRole)
                .bind("eventId", eventId)
                .bind("login", login)
                .map(row -> row.get("role_name", String.class))
                .first()
                .map(roleName ->
                        "Организатор".equalsIgnoreCase(roleName) ||
                                "Создатель".equalsIgnoreCase(roleName)
                )
                .defaultIfEmpty(false);
    }
}
