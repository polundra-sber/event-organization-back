package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.ReceiptEntity;

public interface ReceiptEntityRepository extends R2dbcRepository<ReceiptEntity, Integer> {
}
