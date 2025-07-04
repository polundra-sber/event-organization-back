package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.UserNotEventParticipantException;
import ru.eventorg.exception.WrongUserRoleException;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final R2dbcEntityTemplate template;

    private final String sqlCheckRole = """
            SELECT role.role_name
            FROM event_user_list eul
            JOIN role ON eul.role_id = role.role_id
            WHERE eul.event_id = $1 AND eul.user_id = $2
            """;

    public Mono<Boolean> checkIfCreator(Integer eventId, String login) {
        return template.getDatabaseClient()
                .sql(sqlCheckRole)
                .bind(0, eventId)
                .bind(1, login)
                .map(row -> row.get("role_name", String.class))
                .first()
                .switchIfEmpty(Mono.error(new UserNotEventParticipantException(ErrorState.USER_NOT_EVENT_PARTICIPANT)))
                .flatMap(roleName -> {
                    if ("Создатель".equalsIgnoreCase(roleName)) {
                        return Mono.just(true);
                    } else {
                        return Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_ROLE));
                    }
                });
    }

    public Mono<Boolean> checkIfOrganizerOrHigher(Integer eventId, String login) {
        return template.getDatabaseClient()
                .sql(sqlCheckRole)
                .bind(0, eventId)
                .bind(1, login)
                .map(row -> row.get("role_name", String.class))
                .first()
                .switchIfEmpty(Mono.error(new UserNotEventParticipantException(ErrorState.USER_NOT_EVENT_PARTICIPANT)))
                .flatMap(roleName -> {
                    if ("Организатор".equalsIgnoreCase(roleName) || "Создатель".equalsIgnoreCase(roleName)) {
                        return Mono.just(true);
                    } else {
                        return Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_OR_ORGANIZER_ROLE));
                    }
                });
    }
}
