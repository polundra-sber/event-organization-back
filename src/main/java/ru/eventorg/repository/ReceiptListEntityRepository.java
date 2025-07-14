package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.ReceiptListEntity;

public interface ReceiptListEntityRepository extends R2dbcRepository<ReceiptListEntity, Integer> {
    Mono<Boolean> existsReceiptListEntityByPurchaseId(Integer purchaseId);

    Flux<ReceiptListEntity> findAllByPurchaseId(Integer purchaseId);
}
