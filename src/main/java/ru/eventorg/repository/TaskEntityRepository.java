package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.eventorg.entity.TaskEntity;
import reactor.core.publisher.Mono;
import java.util.List;

public interface TaskEntityRepository  extends R2dbcRepository<TaskEntity, Integer> {
    Mono<TaskEntity> getTaskEntityByEventIdAndTaskId(Integer eventId, Integer taskId);

    Flux<TaskEntity> getTaskEntitiesByEventId(Integer eventId);
}
