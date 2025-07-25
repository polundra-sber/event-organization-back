package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.PurchaseEntity;

@Repository
public interface PurchaseEntityRepository extends R2dbcRepository<PurchaseEntity, Integer> {
    Flux<PurchaseEntity> getPurchaseEntitiesByEventId(Integer eventId);
    Mono<Void> deleteByPurchaseIdAndEventId(Integer purchaseId, Integer eventId);
    Mono<PurchaseEntity> findByPurchaseIdAndEventId(Integer purchaseId, Integer eventId);
    Mono<PurchaseEntity> findByPurchaseIdAndResponsibleUser(Integer purchaseId, String responsibleUser);
    Mono<Boolean> existsPurchaseEntityByPurchaseIdAndEventId(Integer purchaseId, Integer eventId);
    Mono<Boolean> existsPurchaseEntitiesByEventIdAndResponsibleUserIsNull(Integer eventId);
    Mono<Boolean> existsByPurchaseIdAndResponsibleUser(Integer purchaseId, String responsibleUser);

    Flux<PurchaseEntity> findAllByEventId(Integer eventId);
}


