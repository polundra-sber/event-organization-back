package ru.eventorg.repository;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventEntity;

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

    default Mono<String> findEventStatusNameByEventId(Integer eventId, R2dbcEntityTemplate template) {
        String query = """
            SELECT es.event_status_name 
            FROM event e
            JOIN event_status es ON e.status_id = es.event_status_id
            WHERE e.event_id = :eventId
            """;

        return template.getDatabaseClient()
                .sql(query)
                .bind("eventId", eventId)
                .map((row, metadata) -> row.get("event_status_name", String.class))
                .one();
    }
}
