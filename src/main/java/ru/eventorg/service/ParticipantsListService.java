package ru.eventorg.service;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.FullUser;
import ru.eventorg.entity.EventUserListEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.RoleOfCreatorIsUnchangeable;
import ru.eventorg.repository.EventUserListEntityRepository;
import ru.eventorg.repository.RoleEntityRepository;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.enums.UserRole;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantsListService {
    private final R2dbcEntityTemplate template;
    private final EventUserListEntityRepository eventUserListEntityRepository;
    private final EventService eventService;
    private final RoleService roleService;
    private final RoleEntityRepository roleEntityRepository;

    private final int paginationSize = 10;

    public Flux<FullUser> getParticipantsById(Integer eventId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMapMany(login -> roleService.getUserRoleInEvent(eventId, login)
                        .flatMapMany(currentUserRole -> {
                            boolean showAll = UserRole.CREATOR.getDisplayName()
                                    .equalsIgnoreCase(currentUserRole);
                            String sql = """
                    SELECT
                        eul.user_id AS login,
                        r.role_name AS role,
                        us.email AS email,
                        us.password AS password,
                        up.name AS name,
                        up.surname AS surname,
                        up.comment_money_transfer AS commentMoneyTransfer
                    FROM event_user_list eul
                    JOIN user_profile up ON up.login = eul.user_id
                    JOIN user_secret us ON us.login = eul.user_id
                    JOIN role r ON r.role_id = eul.role_id
                    WHERE eul.event_id = $1
                    """ + (showAll ? "" : " AND r.role_name != 'не допущен'");

                            return template.getDatabaseClient()
                                    .sql(sql)
                                    .bind(0, eventId)
                                    .map(this::mapRowToFullUser)
                                    .all();
                        }));
    }

    private FullUser mapRowToFullUser(Row row, RowMetadata metadata) {
        return new FullUser(
                row.get("login", String.class),
                row.get("role", String.class),
                row.get("email", String.class),
                row.get("password", String.class),
                row.get("name", String.class),
                row.get("surname", String.class),
                row.get("commentMoneyTransfer", String.class)
        );
    }

    public Mono<Void> addParticipantsToEvent(Integer eventId, Mono<List<String>> logins) {
        return roleEntityRepository.getRoleIdByRoleName(UserRole.PARTICIPANT.getDisplayName())
                .flatMapMany(participantRoleId ->
                        roleEntityRepository.getRoleIdByRoleName(UserRole.NOT_ALLOWED.getDisplayName())
                                .flatMapMany(notAllowedRoleId ->
                                        logins.flatMapMany(Flux::fromIterable)
                                                .flatMap(login ->
                                                        isUserParticipant(eventId, login)
                                                                .flatMap(isUserParticipant ->{
                                                                    if(isUserParticipant){
                                                                        return Mono.empty();
                                                                    }
                                                                    return processParticipant(eventId, login, participantRoleId, notAllowedRoleId);
                                                                })
                                                )
                                )
                )
                .then();
    }

    private Mono<Void> processParticipant(
            Integer eventId,
            String login,
            Integer participantRoleId,
            Integer notAllowedRoleId
    ) {
        return template.getDatabaseClient()
                .sql("""
                INSERT INTO event_user_list (event_id, user_id, role_id)
                VALUES (:eventId, :login, :participantRoleId)
                ON CONFLICT (event_id, user_id) DO UPDATE
                SET role_id = :participantRoleId
                WHERE event_user_list.role_id = :notAllowedRoleId
                """)
                .bind("eventId", eventId)
                .bind("login", login)
                .bind("participantRoleId", participantRoleId)
                .bind("notAllowedRoleId", notAllowedRoleId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Mono<Boolean> isUserParticipant(Integer eventId, String userId) {
        return eventUserListEntityRepository.getEventUserListEntityByEventIdAndUserId(eventId, userId)
                .flatMap(entry ->
                        roleEntityRepository.findById(entry.getRoleId())
                                .map(role -> {
                                    String roleName = role.getRoleName();
                                    return roleName.equals(UserRole.PARTICIPANT.getDisplayName()) ||
                                            roleName.equals(UserRole.CREATOR.getDisplayName()) ||
                                            roleName.equals(UserRole.ORGANIZER.getDisplayName());
                                })
                )
                .defaultIfEmpty(false);
    }

    public Flux<FullUser> searchUsersByNameSurnameEmail(Integer eventId, String searchText, Integer sequenceNumber) {
        final int limit = paginationSize;
        final int offset = sequenceNumber * limit;
        final String wrappedSearchText = "%" + searchText.toLowerCase() + "%";

        String sql = """
        SELECT
            up.login AS login,
            up.name AS name,
            up.surname AS surname,
            us.email AS email,
            us.password AS password,
            up.comment_money_transfer AS commentMoneyTransfer
        FROM user_profile up
        JOIN user_secret us ON us.login = up.login
        WHERE (
                LOWER(up.name)    LIKE LOWER(:txt)
             OR LOWER(up.surname) LIKE LOWER(:txt)
             OR LOWER(us.email)   LIKE LOWER(:txt)
        )
          AND up.login NOT IN (
              SELECT user_id
              FROM event_user_list
              WHERE event_id = :eid
          )
        ORDER BY up.surname ASC, up.name ASC
        OFFSET :ofst
        LIMIT :lmt;
        """;

        return template.getDatabaseClient()
                .sql(sql)
                .bind("txt", wrappedSearchText)
                .bind("eid", eventId)
                .bind("ofst", offset)
                .bind("lmt", limit)
                .map((row, metadata) -> {
                    FullUser fullUser = new FullUser();
                    fullUser.setLogin(row.get("login", String.class));
                    fullUser.setName(row.get("name", String.class));
                    fullUser.setSurname(row.get("surname", String.class));
                    fullUser.setEmail(row.get("email", String.class));
                    fullUser.setPassword(row.get("password", String.class));
                    fullUser.setCommentMoneyTransfer(row.get("commentMoneyTransfer", String.class));
                    return fullUser;
                })
                .all();
    }


    public Mono<Void> deleteParticipantFromParticipantsList(Integer eventId, String participantLogin) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(currentUserLogin -> {
                    // 1. Проверяем что мероприятие существует и активно
                    return eventService.validateExists(eventId)
                            .then(eventService.validateEventIsActive(eventId))

                            // 2. Проверяем что текущий пользователь - организатор или создатель
                            .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))

                            // 3. Проверяем что удаляемый пользователь - участник мероприятия
                            .then(roleService.validateIsParticipant(eventId, participantLogin))

                            // 4. Удаляем участника
                            .then(eventUserListEntityRepository.deleteEventUserListEntityByEventIdAndUserId(eventId, participantLogin))
                            .then();
                });
    }

    public Mono<String> changeParticipantRole(Integer eventId, String participantLogin) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(currentUserLogin -> {
                    // 1. Проверяем что мероприятие существует и активно
                    return eventService.validateExists(eventId)
                            .then(eventService.validateEventIsActive(eventId))

                            // 2. Проверяем что текущий пользователь - создатель
                            .then(roleService.checkIfCreator(eventId, currentUserLogin))

                            // 3. Проверяем что participant - участник (но не создатель)
                            .then(validateParticipantIsNotCreator(eventId, participantLogin))

                            // 4. Получаем текущую роль участника
                            .then(roleService.getUserRoleInEvent(eventId, participantLogin))

                            // 5. Определяем новую роль и обновляем
                            .flatMap(currentRole -> {
                                String newRole = determineNewRole(currentRole);
                                return updateParticipantRole(eventId, participantLogin, newRole)
                                        .thenReturn(newRole);
                            });
                });
    }

    private Mono<Void> validateParticipantIsNotCreator(Integer eventId, String participantLogin) {
        return roleService.getUserRoleInEvent(eventId, participantLogin)
                .flatMap(role -> {
                    if (UserRole.CREATOR.getDisplayName().equalsIgnoreCase(role)) {
                        return Mono.error(new RoleOfCreatorIsUnchangeable(ErrorState.ROLE_IS_UNCHANGEABLE));
                    }
                    return Mono.empty();
                });
    }

    private String determineNewRole(String currentRole) {
        if (UserRole.ORGANIZER.getDisplayName().equalsIgnoreCase(currentRole)) {
            return UserRole.PARTICIPANT.getDisplayName();
        } else if (UserRole.PARTICIPANT.getDisplayName().equalsIgnoreCase(currentRole)) {
            return UserRole.ORGANIZER.getDisplayName();
        }
        throw new IllegalStateException(currentRole);
    }

    private Mono<Long> updateParticipantRole(Integer eventId, String participantLogin, String newRole) {
        String sql = """
        UPDATE event_user_list eul
        SET role_id = (SELECT role_id FROM role WHERE role_name = :newRole)
        WHERE eul.event_id = :eventId 
        AND eul.user_id = :participantLogin
        """;

        return template.getDatabaseClient()
                .sql(sql)
                .bind("eventId", eventId)
                .bind("participantLogin", participantLogin)
                .bind("newRole", newRole)
                .fetch()
                .rowsUpdated();
    }

}
