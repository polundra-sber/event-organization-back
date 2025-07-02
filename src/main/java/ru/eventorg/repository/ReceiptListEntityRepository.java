package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.ReceiptListEntity;

public interface ReceiptListEntityRepository extends R2dbcRepository<ReceiptListEntity, Integer> {
}
