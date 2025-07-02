package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.eventorg.entity.PurchaseEntity;

@Repository
public interface PurchaseEntityRepository extends R2dbcRepository<PurchaseEntity, Integer> {

}
