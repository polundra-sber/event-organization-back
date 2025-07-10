package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.eventorg.entity.DebtStatusEntity;
import reactor.core.publisher.Mono;
import java.util.List;

public interface DebtStatusEntityRepository extends R2dbcRepository<DebtStatusEntity, Integer> {
    Mono<DebtStatusEntity> getDebtStatusEntityByDebtStatusName(String debtStatusName);
}
