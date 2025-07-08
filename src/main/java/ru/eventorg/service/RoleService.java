package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.UserNotEventParticipantException;
import ru.eventorg.exception.WrongUserRoleException;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.service.enums.UserRole;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final R2dbcEntityTemplate template;
    private final EventEntityRepository eventEntityRepository;

    private final String SQL_CHECK_ROLE = """
            SELECT role.role_name
            FROM event_user_list eul
            JOIN role ON eul.role_id = role.role_id
            WHERE eul.event_id = $1 AND eul.user_id = $2
            """;

    public Mono<Boolean> checkIfCreator(Integer eventId, String login) {
        return getUserRoleInEvent(eventId, login)
                .flatMap(roleName -> {
                    if (UserRole.CREATOR.getDisplayName().equalsIgnoreCase(roleName)) {
                        return Mono.just(true);
                    }
                    return Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_ROLE));
                });
    }

    public Mono<Boolean> checkIfOrganizerOrHigher(Integer eventId, String login) {
        return getUserRoleInEvent(eventId, login)
                .flatMap(roleName -> {
                    if (UserRole.ORGANIZER.getDisplayName().equalsIgnoreCase(roleName)
                            || UserRole.CREATOR.getDisplayName().equalsIgnoreCase(roleName)) {
                        return Mono.just(true);
                    }
                    return Mono.error(new WrongUserRoleException(
                            ErrorState.NOT_CREATOR_OR_ORGANIZER_ROLE));
                });
    }

    /**
     * Возвращает роль пользователя в рамках указанного мероприятия.
     * Если пользователь не участвует в событии, бросает UserNotEventParticipantException
     *
     * @param eventId идентификатор мероприятия
     * @param login   логин пользователя
     * @return Mono с названием роли
     */
    public Mono<String> getUserRoleInEvent(Integer eventId, String login) {
        return template.getDatabaseClient()
                .sql(SQL_CHECK_ROLE)
                .bind(0, eventId)
                .bind(1, login)
                .map((row, meta) -> row.get("role_name", String.class))
                .first()
                .switchIfEmpty(
                        Mono.error(new UserNotEventParticipantException(
                                ErrorState.USER_NOT_EVENT_PARTICIPANT))
                );
    }

    public Mono<Void> validateIsParticipant(Integer eventId, String userLogin) {
        if (userLogin == null) {
            return Mono.empty();
        }

        return getUserRoleInEvent(eventId, userLogin)
                .flatMap(roleName -> {
                    if (UserRole.NOT_ALLOWED.getDisplayName().equals(roleName)) {
                        return Mono.error(new UserNotEventParticipantException(ErrorState.USER_NOT_EVENT_PARTICIPANT));
                    }
                    return Mono.empty();
                });
    }
}