package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.FullUser;
import ru.eventorg.entity.EventUserListEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.entity.UserSecretEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.EventNotExistException;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.EventUserListEntityRepository;
import ru.eventorg.repository.UserProfilesEntityRepository;
import ru.eventorg.repository.UserSecretsEntityRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantsListService {
    private final R2dbcEntityTemplate template;
    private final EventUserListEntityRepository eventUserListEntityRepository;
    private final EventEntityRepository eventEntityRepository;

    private final int paginationSize = 10;

    public Flux<FullUser> getParticipantsById(Integer eventId) {
        String sql = """
            SELECT
                eul.user_id AS login,
                eul.role_id AS role,
                us.email       AS email,
                us.password    AS password,
                up.name        AS name,
                up.surname     AS surname,
                up.comment_money_transfer AS commentMoneyTransfer
            FROM event_user_list eul
            JOIN user_profile up ON up.login = eul.user_id
            JOIN user_secret  us ON us.login = eul.user_id
            WHERE eul.event_id = $1
            """;

        return template.getDatabaseClient()
                .sql(sql)
                .bind(0, eventId)
                .map((row, metadata) -> new FullUser(
                        row.get("login", String.class),
                        row.get("role", Integer.class),
                        row.get("email", String.class),
                        row.get("password", String.class),
                        row.get("name", String.class),
                        row.get("surname", String.class),
                        row.get("commentMoneyTransfer", String.class)
                ))
                .all()
                .switchIfEmpty(Mono.error(
                        new EventNotExistException(ErrorState.EVENT_NOT_EXIST)
                ));
    }

    public Mono<Void> addParticipantsToEvent(Integer eventId, Mono<List<String>> logins) {
        return eventEntityRepository.existsEventEntityByEventId(eventId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST));
                    }

                    return template.getDatabaseClient().sql(
                                    "SELECT role_id FROM role WHERE role_name = $1"
                            )
                            .bind(0, "Участник")
                            .map((row, meta) -> row.get("role_id", Integer.class))
                            .one()
                            .flatMapMany(roleId ->
                                    logins
                                            .flatMapMany(Flux::fromIterable)
                                            .flatMap(login ->
                                                    eventUserListEntityRepository.existsByEventIdAndUserId(eventId, login)
                                                            .flatMap(existsUserInEvent -> {
                                                                log.info("login {} existsInEvent: {}", login, existsUserInEvent);
                                                                if (existsUserInEvent) {
                                                                    return Mono.empty();   // Уже участник – пропускаем
                                                                }
                                                                EventUserListEntity entity = new EventUserListEntity(
                                                                        null, eventId, login, roleId
                                                                );
                                                                return eventUserListEntityRepository.save(entity).then();
                                                            })
                                            )
                            )
                            .then();  // вернуть Mono<Void> по завершении всех вставок
                });
    }

    public Flux<FullUser> searchUsersByNameSurnameEmail(Integer eventId, String searchText, Integer sequenceNumber) {
        return eventEntityRepository.existsEventEntityByEventId(eventId)
                .flatMapMany(exists -> {
                    if (!exists) {
                        return Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST));
                    }

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
                });
    }
}
