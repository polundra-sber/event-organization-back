package ru.eventorg.service;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.AllArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.DebtIncomesCustom;
import ru.eventorg.exception.*;
import ru.eventorg.repository.DebtEntityRepository;
import ru.eventorg.repository.DebtStatusEntityRepository;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.security.SecurityUtils;

@Service
@AllArgsConstructor
public class MyIncomesService {
    private final R2dbcEntityTemplate template;
    private final DebtEntityRepository debtEntityRepository;
    private final EventService eventService;
    private final DebtStatusEntityRepository debtStatusEntityRepository;

    public Flux<DebtIncomesCustom> getDebtIncomesCustom() {
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
                                ds.debt_status_name,
                                d.debt_amount
                            FROM debt d
                                     JOIN event e ON e.event_id = d.event_id
                                     JOIN user_profile up ON up.login = d.recipient_id
                                     JOIN debt_status ds ON d.status_id = ds.debt_status_id
                            WHERE d.recipient_id = :login
                        """;
                    return template.getDatabaseClient()
                            .sql(query)
                            .bind("login", login)
                            .map(this::mapRowToDebtIncomesCustom)
                            .all();
                });
    }

    private DebtIncomesCustom mapRowToDebtIncomesCustom(Row row, RowMetadata metadata) {
        return new DebtIncomesCustom(
                row.get("event_id", Integer.class),
                row.get("event_name", String.class),
                row.get("debt_id", Integer.class),
                row.get("login", String.class),
                row.get("name", String.class),
                row.get("surname", String.class),
                row.get("debt_status_name", String.class),
                row.get("debt_amount", Float.class)
        );
    }

    public Mono<Void> markIncomeReceived(Integer debtId) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login ->
                        validateDebtExists(debtId)
                                .then(validateDebtRecipient(debtId, login))
                                .then(eventService.validateEventIsActiveForDebt(debtId))
                                .then(debtStatusEntityRepository.getDebtStatusEntityByDebtStatusName("получено")
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


    private Mono<Void> validateDebtRecipient(Integer debtId, String username) {
        return debtEntityRepository.isUserRecipient(debtId, username)
                .flatMap(isResponsible -> isResponsible
                        ? Mono.empty()
                        : Mono.error(new UserNotRecipientException(ErrorState.NOT_RECIPIENT)));
    }
}