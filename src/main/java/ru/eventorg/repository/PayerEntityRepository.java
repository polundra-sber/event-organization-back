package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.PayerEntity;

public interface PayerEntityRepository extends R2dbcRepository<PayerEntity, Integer> {
}
