package ru.eventorg.service;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.StuffWithEventDto;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.StuffNotExistException;
import ru.eventorg.repository.StuffEntityRepository;

import java.util.Map;

import static ru.eventorg.security.SecurityUtils.getCurrentUserLogin;

@Service
public class MyStuffsListService {
    private final DatabaseClient databaseClient;
    private final EventService eventService;
    private final StuffEntityRepository stuffEntityRepository;
    private static final String GET_STUFFS_WITH_EVENTS_SQL = """
        SELECT
           e.event_id,
           e.event_name,
           s.stuff_id,
           s.stuff_name,
           s.stuff_description,
           s.responsible_user
        FROM stuff s
        JOIN event e ON s.event_id = e.event_id
        JOIN event_status es ON e.status_id = es.event_status_id
        WHERE s.responsible_user = $1
        AND es.event_status_name = 'Активно'
        """;

    private static final String DENY_STUFF_SQL = """
    UPDATE stuff
    SET responsible_user = NULL
    WHERE stuff_id = $1 AND responsible_user = $2
    """;

    public MyStuffsListService(DatabaseClient databaseClient, EventService eventService, StuffEntityRepository stuffEntityRepository) {
        this.databaseClient = databaseClient;
        this.eventService = eventService;
        this.stuffEntityRepository = stuffEntityRepository;
    }

    public Flux<StuffWithEventDto> getMyStuffsList() {
        return getCurrentUserLogin()
                .flatMapMany(userLogin ->
                        databaseClient.sql(GET_STUFFS_WITH_EVENTS_SQL)
                                .bind(0, userLogin)
                                .fetch()
                                .all()
                                .flatMap(this::mapRowToStuffWithEventDto)
                                .switchIfEmpty(Flux.empty())
                );
    }


    public Mono<Void> denyStuffInMyStuffsList(Integer stuffId) {
        return getCurrentUserLogin()
                .flatMap(userLogin ->
                        stuffEntityRepository.findByStuffIdAndResponsibleUser(stuffId, userLogin)
                                .switchIfEmpty(Mono.error(new StuffNotExistException(ErrorState.STUFF_NOT_EXIST)))
                                .flatMap(stuff ->
                                        eventService.validateEventIsActive(stuff.getEventId())
                                                .then(
                                                        databaseClient.sql(DENY_STUFF_SQL)
                                                                .bind(0, stuffId)
                                                                .bind(1, userLogin)
                                                                .then()
                                                )
                                )
                );
    }


    //Вспомогательные методы
    private Mono<StuffWithEventDto> mapRowToStuffWithEventDto(Map<String, Object> row) {
        StuffWithEventDto dto = new StuffWithEventDto();
        dto.setEventId((Integer) row.get("event_id"));
        dto.setEventName((String) row.get("event_name"));
        dto.getStuff().setStuffId((Integer) row.get("stuff_id"));
        dto.getStuff().setStuffName((String) row.get("stuff_name"));
        dto.getStuff().setStuffDescription((String) row.get("stuff_description"));
        return Mono.just(dto);
    }
}
