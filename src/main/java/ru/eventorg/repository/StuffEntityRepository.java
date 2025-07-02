package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.StuffEntity;

public interface StuffEntityRepository extends R2dbcRepository<StuffEntity, Integer> {
}
