package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventStatusEntity;


public interface EventStatusEntityRepository extends R2dbcRepository<EventStatusEntity, Integer> {
    Mono<EventStatusEntity> getEventStatusByEventStatusId(Integer eventStatusId);
}
