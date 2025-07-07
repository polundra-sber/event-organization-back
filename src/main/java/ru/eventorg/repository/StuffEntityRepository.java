package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.StuffEntity;

@Repository
public interface StuffEntityRepository extends R2dbcRepository<StuffEntity, Integer> {
    Mono<StuffEntity> findByStuffIdAndEventId(Integer stuffId, Integer eventId);
    Mono<Void> deleteByStuffIdAndEventId(Integer stuffId, Integer eventId);
}
