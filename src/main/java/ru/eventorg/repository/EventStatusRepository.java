package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventStatus;


public interface EventStatusRepository extends R2dbcRepository<EventStatus, String> {
    Mono<EventStatus> getEventStatusByEventStatusId(Integer eventStatusId);
}
