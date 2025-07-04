package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventUserListEntity;

public interface EventUserListEntityRepository extends R2dbcRepository<EventUserListEntity, String> {
    Flux<EventUserListEntity> findEventUserListByUserId(String userId);
    Mono<Boolean> existsEventIdByEventIdAndUserId(Integer eventId, String userId);

}
