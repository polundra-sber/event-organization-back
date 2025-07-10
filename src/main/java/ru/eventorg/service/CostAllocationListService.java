package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.PayerEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.UserNotPurchaseParticipantException;
import ru.eventorg.repository.EventEntityRepository;
import ru.eventorg.repository.PayerEntityRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CostAllocationListService {
    private final R2dbcEntityTemplate template;
    private final EventEntityRepository eventEntityRepository;
    private final PayerEntityRepository payerEntityRepository;

    /**
     * Распределение расходов между участниками мероприятия
     * <p>
     * Алгоритм работы:
     * <ol>
     * <li> Вставляем такие строки в debt:</li>
     * <ol>
     * <li> Получаем из purchase_with_payer_view все записи для заданного eventId</li>
     * <li> Агрегируем сумму переводов в каждом направлении</li>
     * <li> Собираем зеркальные пары (payer+recipient и recipient+payer)</li>
     * <li> Рассчитываем суммы для "зеркальных" пар</li>
     * <li> Собираем остальные (непарные) записи</li>
     * <li> Объединяем оба набора</li>
     * </ol>
     * <li>Устанавливаем cost_allocated на true в таблице event</li>
     * </ol>
     *
     * @param eventId id мероприятия
     */
    public Mono<Void> allocateDebtsBetweenParticipants(Integer eventId) {
        final String sqlAllocateDebts = """
                INSERT INTO debt (payer_id, recipient_id, debt_amount, event_id, status_id)
                WITH filtered_events AS (
                    SELECT *
                    FROM purchase_with_payer_view
                    WHERE event_id = :eventId
                ),
                
                -- 2)
                     aggregated AS (
                         SELECT
                             payer,
                             recipient,
                             SUM(cost) AS total_out
                         FROM filtered_events
                         GROUP BY payer, recipient
                     ),
                
                -- 3)
                     paired AS (
                         SELECT
                             a.payer    AS p1,
                             a.recipient AS r1,
                             a.total_out AS sum1,
                             b.total_out AS sum2
                         FROM aggregated a
                                  JOIN aggregated b
                                       ON a.payer    = b.recipient
                                           AND a.recipient = b.payer
                         WHERE a.payer < a.recipient  -- чтобы обрабатывать каждую пару лишь один раз
                     ),
                
                -- 4)
                     paired_net AS (
                         SELECT
                             CASE
                                 WHEN sum1 > sum2 THEN r1
                                 ELSE p1
                                 END AS payer,
                             CASE
                                 WHEN sum1 > sum2 THEN p1
                                 ELSE r1
                                 END AS recipient,
                             ABS(sum1 - sum2) AS net_amount
                         FROM paired
                         WHERE sum1 <> sum2        -- исключаем полностью взаимокомпенсированные пары
                     ),
                
                -- 5)
                     unpaired AS (
                         SELECT
                             payer,
                             recipient,
                             total_out AS net_amount
                         FROM aggregated a
                         WHERE NOT EXISTS (
                             SELECT 1
                             FROM aggregated b
                             WHERE b.payer    = a.recipient
                               AND b.recipient = a.payer
                         )
                     )
                
                -- 6)
                SELECT payer, recipient, net_amount, :eventId AS event_id, :statusId AS status_id
                FROM paired_net
                
                UNION ALL
                
                SELECT payer, recipient, net_amount, :eventId AS event_id, :statusId AS status_id
                FROM unpaired;
                """;

        return template.getDatabaseClient()
                .sql("SELECT debt_status_id FROM debt_status WHERE debt_status_name = :name")
                .bind("name", "не оплачено")
                .map((row, meta) -> row.get("debt_status_id", Integer.class))
                .one()
                .flatMap(statusId ->
                        template.getDatabaseClient()
                                .sql(sqlAllocateDebts)
                                .bind("eventId", eventId)
                                .bind("statusId", statusId)
                                .fetch()
                                .rowsUpdated()
                                .then(eventEntityRepository.markCostAllocated(eventId))
                ).then();
    }

    public Mono<Void> addParticipantsToPurchase(Integer purchaseId, Mono<List<String>> logins) {
        return logins.flatMapMany(Flux::fromIterable)
                .flatMap(login ->
                        payerEntityRepository.existsByPurchaseIdAndUserId(purchaseId, login)
                                .flatMap(exists -> {
                                    if (exists) {
                                        // Уже участник — пропускаем
                                        return Mono.empty();
                                    }
                                    // Добавляем нового плательщика
                                    PayerEntity entity = new PayerEntity(null, purchaseId, login);
                                    return payerEntityRepository.save(entity).then();
                                })
                )
                .then();
    }

    public Mono<Void> removeParticipantsFromPurchase(Integer purchaseId, Mono<List<String>> loginsMono) {
        return loginsMono
                .flatMap(logins ->
                        Flux.fromIterable(logins)
                                .flatMap(login ->
                                        payerEntityRepository.existsByPurchaseIdAndUserId(purchaseId, login)
                                                .flatMap(exists -> {
                                                    if (!exists) {
                                                        return Mono.error(new UserNotPurchaseParticipantException(ErrorState.USER_NOT_PURCHASE_PARTICIPANT));
                                                    }
                                                    return Mono.just(login);
                                                })
                                )
                                .collectList()
                                .flatMap(validLogins ->
                                        Flux.fromIterable(validLogins)
                                                .flatMap(login -> payerEntityRepository.deleteByPurchaseIdAndUserId(purchaseId, login))
                                                .then()
                                )
                );
    }
}
