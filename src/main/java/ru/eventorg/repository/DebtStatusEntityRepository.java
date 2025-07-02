package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.DebtStatusEntity;

public interface DebtStatusEntityRepository extends R2dbcRepository<DebtStatusEntity, Integer> {
}
