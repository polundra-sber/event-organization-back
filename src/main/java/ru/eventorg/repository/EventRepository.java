package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.Event;

public interface EventRepository extends R2dbcRepository<Event, String> {
    Mono<Event> findEventByEventId(String eventId);
}
