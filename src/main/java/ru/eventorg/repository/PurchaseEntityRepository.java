package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.eventorg.entity.PurchaseEntity;

import java.util.List;

@Repository
public interface PurchaseEntityRepository extends R2dbcRepository<PurchaseEntity, Integer> {
    Flux<PurchaseEntity> getPurchaseEntitiesByEventId(Integer eventId);
}
