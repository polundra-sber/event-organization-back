package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.TaskEntity;

public interface TaskEntityRepository  extends R2dbcRepository<TaskEntity, Integer> {
}
