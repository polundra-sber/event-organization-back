package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.DebtEntity;

public interface DebtEntityRepository extends R2dbcRepository<DebtEntity, Integer> {

}
