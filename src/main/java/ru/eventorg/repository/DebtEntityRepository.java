package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.DebtEntity;

public interface DebtEntityRepository extends R2dbcRepository<DebtEntity, Integer> {

    @Query("SELECT EXISTS(SELECT 1 FROM debt WHERE debt_id = :debtId AND recipient_id = :username)")
    Mono<Boolean> isUserRecipient(@Param("debtId") Integer debtId, @Param("username") String username);

    @Query("SELECT EXISTS(SELECT 1 FROM debt WHERE debt_id = :debtId AND payer_id = :username)")
    Mono<Boolean> isUserPayer(@Param("debtId") Integer debtId, @Param("username") String username);

    @Query("SELECT event_id FROM debt WHERE debt_id = :debtId")
    Mono<Integer> findEventIdByDebtId(@Param("debtId") Integer debtId);

    @Modifying
    @Query("UPDATE debt SET status_id = :statusId WHERE debt_id = :debtId")
    Mono<Integer> updateDebtStatus(@Param("debtId") Integer debtId, @Param("statusId") Integer statusId);
}
