package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.TaskStatusEntity;

public interface TaskStatusEntityRepository  extends R2dbcRepository<TaskStatusEntity, Integer> {
}
