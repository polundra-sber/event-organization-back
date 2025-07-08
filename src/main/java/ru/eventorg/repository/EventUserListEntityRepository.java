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
    @Query("UPDATE event_user_list SET role_id = :roleId WHERE role_id = :oldId")
    Mono<Integer> updateRoleId(@Param("oldId") Integer oldId, @Param("roleId") Integer roleId);
}
