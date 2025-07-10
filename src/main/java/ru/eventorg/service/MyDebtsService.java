package ru.eventorg.service;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.DebtCustom;
import ru.eventorg.exception.*;
import ru.eventorg.repository.DebtEntityRepository;
import ru.eventorg.repository.DebtStatusEntityRepository;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.security.SecurityUtils;

@Service
@AllArgsConstructor
@Slf4j
public class MyDebtsService {

    private final R2dbcEntityTemplate template;
    private final DebtEntityRepository debtEntityRepository;
    private final EventService eventService;
    private final DebtStatusEntityRepository  debtStatusEntityRepository;

    public Flux<DebtCustom> getDebtCustom() {
        log.info("in getDebtCustom");
        return SecurityUtils.getCurrentUserLogin()
                .flatMapMany(login -> {
                    String query = """
                            SELECT
                                e.event_id,
                                e.event_name,
                                d.debt_id,
                                up.login,
                                up.name,
                                up.surname,
                                up.comment_money_transfer,
                                ds.debt_status_name,
                                d.debt_amount
                            FROM debt d
                                     JOIN event e ON e.event_id = d.event_id
                                     JOIN user_profile up ON up.login = d.payer_id
                                     JOIN debt_status ds ON d.status_id = ds.debt_status_id
                            WHERE d.payer_id = :login
                        """;
                    return template.getDatabaseClient()
                            .sql(query)
                            .bind("login", login)
                            .map(this::mapRowToDebtCustom)
                            .all();
                });
    }

    private DebtCustom mapRowToDebtCustom(Row row, RowMetadata metadata) {
        return new DebtCustom(
                row.get("event_id", Integer.class),
                row.get("event_name", String.class),
                row.get("debt_id", Integer.class),
                row.get("login", String.class),
                row.get("name", String.class),
                row.get("surname", String.class),
                row.get("comment_money_transfer", String.class),
                row.get("debt_status_name", String.class),
                row.get("debt_amount", Float.class)
        );
    }

    public Mono<Void> markDebtPaid(Integer debtId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login ->
                        validateDebtExists(debtId)
                                .then(validateDebtPayer(debtId, login))
                                .then(eventService.validateEventIsActiveForDebt(debtId))
                                .then(debtStatusEntityRepository.getDebtStatusEntityByDebtStatusName("Оплачено")
                                        .flatMap(paidStatus ->
                                                debtEntityRepository.updateDebtStatus(debtId, paidStatus.getDebtStatusId())
                                        )
                                )
                )
                .then();
    }


    private Mono<Void> validateDebtExists(Integer debtId) {
        return debtEntityRepository.existsById(debtId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new DebtNotExistsException(ErrorState.DEBT_NOT_EXIST)));
    }


    private Mono<Void> validateDebtPayer(Integer debtId, String username) {
        return debtEntityRepository.isUserPayer(debtId, username)
                .flatMap(isResponsible -> isResponsible
                        ? Mono.empty()
                        : Mono.error(new UserNotPayerException(ErrorState.NOT_PAYER)));
    }
}
