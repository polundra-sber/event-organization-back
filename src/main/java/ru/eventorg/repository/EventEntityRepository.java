package ru.eventorg.repository;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventEntity;

import java.time.LocalDate;
import java.util.List;

public interface EventEntityRepository extends R2dbcRepository<EventEntity, Integer> {
    Mono<EventEntity> getEventByEventId(Integer eventId);
    Mono<Boolean> existsEventEntityByEventId(Integer eventId);

    @Query("""
    SELECT e.* FROM event e
    WHERE e.event_id = :eventId AND e.status_id in (1, 2)
    """)
    Mono<EventEntity> getActiveOrCompletedEventById(@Param("eventId") Integer eventId);

    @Query("""
    SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
    FROM event e
    WHERE e.event_id = :eventId AND e.status_id = 1
    """)
    Mono<Boolean> existsActiveEventById(@Param("eventId") Integer eventId);

    @Query("""
    SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
    FROM event e
    WHERE e.event_id = :eventId AND e.status_id != 3
    """)
    Mono<Boolean> existsActiveOrCompletedEvent(@Param("eventId") Integer eventId);

    @Query("""
    SELECT cost_allocated
    FROM event
    WHERE event_id = :eventId
    """)
    Mono<Boolean> isEventCostAllocated(@Param("eventId") Integer eventId);

    @Modifying
    @Query("""
    UPDATE event
       SET cost_allocated = TRUE
     WHERE event_id = :eventId
    """)
    Mono<Integer> markCostAllocated(@Param("eventId") Integer eventId);

    Flux<EventEntity> findByStatusIdInAndEventDateBefore(List<Integer> statusIds, LocalDate before);
}
