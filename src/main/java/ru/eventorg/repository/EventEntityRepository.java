package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventEntity;

public interface EventEntityRepository extends R2dbcRepository<EventEntity, Integer> {
    Mono<EventEntity> findEventByEventId(Integer eventId);
    Mono<Boolean> existsEventEntityByEventId(Integer eventId);
}
