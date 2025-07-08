package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.EventNotActiveException;
import ru.eventorg.exception.EventNotExistException;
import ru.eventorg.repository.EventEntityRepository;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventEntityRepository eventEntityRepository;
    private final R2dbcEntityTemplate template;

    private static final String SQL_GET_STATUS = """
        SELECT event_status.event_status_name
        FROM event JOIN event_status
        ON event.status_id = event_status.event_status_id
        WHERE event.event_id = $1
        """;

    /**
     * Возвращает название статуса мероприятия по его идентификатору.
     * Если мероприятия не существует — выбрасывает EventNotExistException.
     *
     * @param eventId идентификатор мероприятия
     * @return Mono с названием статуса
     */
    public Mono<String> getEventStatus(Integer eventId) {
        return eventEntityRepository.existsEventEntityByEventId(eventId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST));
                    }
                    return template.getDatabaseClient()
                            .sql(SQL_GET_STATUS)
                            .bind(0, eventId)
                            .map((row, meta) -> row.get("event_status_name", String.class))
                            .first();
                });
    }

    public Mono<Void> validateExists(Integer eventId) {
        return eventEntityRepository.existsById(eventId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST)));
    }


    public Mono<EventEntity> getActiveOrCompletedEvent(Integer eventId) {
        return eventEntityRepository.getActiveOrCompletedEventById(eventId)
                .switchIfEmpty(Mono.error(new EventNotExistException(ErrorState.EVENT_NOT_EXIST)));
    }

    public Mono<Void> validateEventIsActive(Integer eventId) {
        return eventEntityRepository.existsActiveEventById(eventId)
                .flatMap(isActive -> isActive
                        ? Mono.empty()
                        : Mono.error(new EventNotActiveException(ErrorState.EVENT_NOT_ACTIVE)));
    }

    public Mono<Boolean> isCostAllocated(Integer eventId) {
        return eventEntityRepository.isEventCostAllocated(eventId);
    }
}
