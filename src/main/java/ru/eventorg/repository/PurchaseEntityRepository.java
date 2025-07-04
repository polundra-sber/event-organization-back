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

    @Query("""
        SELECT 
            p.purchase_id, p.purchase_name, p.purchase_description,
            p.responsible_user, p.event_id,
            up.login AS user_login,
            up.name AS user_name,
            up.surname AS user_surname,
            up.comment_money_transfer AS comment_money_transfer
        FROM purchase p
        LEFT JOIN user_profile up ON p.responsible_user = up.login
        WHERE p.event_id = $1
        """)
    Flux<PurchaseWithResponsibleUserProjection> findPurchasesWithUserByEventId(Integer eventId);

}


