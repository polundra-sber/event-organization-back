package ru.eventorg.repository;

import io.micrometer.common.lang.NonNull;
import io.micrometer.common.lang.NonNullApi;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import ru.eventorg.entity.TaskEntity;
import reactor.core.publisher.Mono;
import java.util.List;

public interface TaskEntityRepository  extends R2dbcRepository<TaskEntity, Integer> {
    Mono<TaskEntity> getTaskEntityByEventIdAndTaskId(Integer eventId, Integer taskId);

    Flux<TaskEntity> getTaskEntitiesByEventId(Integer eventId);

    @Modifying
    @Query("UPDATE task SET responsible_user = NULL WHERE task_id = :taskId")
    Mono<Integer> updateResponsibleUser(@Param("taskId") Integer taskId);

    @Modifying
    @Query("UPDATE task SET status_id = :statusId WHERE task_id = :taskId")
    Mono<Integer> updateTaskStatus(@Param("taskId") Integer taskId, @Param("statusId") Integer statusId);

    @Query("SELECT EXISTS(SELECT 1 FROM task WHERE task_id = :taskId AND responsible_user = :username)")
    Mono<Boolean> isUserResponsible(@Param("taskId") Integer taskId, @Param("username") String username);

    @Query("SELECT event_id FROM task WHERE task_id = :taskId")
    Mono<Integer> findEventIdByTaskId(@Param("taskId") Integer taskId);

    @Query("SELECT status_id FROM task WHERE task_id = :taskId")
    Mono<Integer> findTaskStatusById(@Param("taskId") Integer taskId);

}
