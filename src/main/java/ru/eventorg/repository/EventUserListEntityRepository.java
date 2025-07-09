package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventUserListEntity;

public interface EventUserListEntityRepository extends R2dbcRepository<EventUserListEntity, String> {
    Mono<EventUserListEntity> deleteEventUserListEntityByEventIdAndUserId(Integer eventId, String userId);
    Mono<Boolean> existsEventIdByEventIdAndUserId(Integer eventId, String userId);
    Mono<EventUserListEntity> getEventUserListEntityByEventIdAndUserId(Integer eventId, String userId);

    @Modifying
    @Query("UPDATE event_user_list SET role_id = :roleId WHERE event_user_list_id = :id")
    Mono<Integer> updateRoleId(@Param("id") Integer id, @Param("roleId") Integer roleId);

    @Query("SELECT EXISTS(SELECT 1 FROM event_user_list " +
            "WHERE event_id = :eventId AND user_id = :userId AND role_id = :roleId)")
    Mono<Boolean> existsByEventIdAndUserIdAndRoleId(
            @Param("eventId") Integer eventId,
            @Param("userId") String userId,
            @Param("roleId") Integer roleId
    );
}
