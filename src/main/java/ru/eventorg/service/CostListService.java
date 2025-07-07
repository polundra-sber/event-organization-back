package ru.eventorg.service;

import io.r2dbc.spi.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.FullUser;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.repository.PayerEntityRepository;
import ru.eventorg.repository.PurchaseEntityRepository;
import ru.eventorg.repository.ReceiptListEntityRepository;

@Service
@RequiredArgsConstructor
public class CostListService {
    private final ReceiptListEntityRepository receiptListEntityRepository;
    private final EventValidationService eventValidationService;
    private final PayerEntityRepository payerEntityRepository;
    private final PurchaseEntityRepository purchaseEntityRepository;
    private final R2dbcEntityTemplate template;
    private final ResourceLoader resourceLoader;

    public Mono<Boolean> hasReceipt(Integer purchaseId) {
        return receiptListEntityRepository.existsReceiptListEntityByPurchaseId(purchaseId);
    }

    public Flux<FullUser> getPayersForPurchase(Integer eventId, Integer purchaseId) {
        String sqlGetPayers = """
                SELECT
                    up.login       AS login,
                    us.email       AS email,
                    us.password    AS password,
                    up.name        AS name,
                    up.surname     AS surname,
                    up.comment_money_transfer AS commentMoneyTransfer
                FROM payer p
                JOIN user_profile up ON up.login = p.user_id
                JOIN user_secret  us ON us.login = p.user_id
                WHERE p.purchase_id = $1
                """;

        return eventValidationService.validateExists(eventId)
                .then()
                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId)
                        .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                )
                .thenMany(template.getDatabaseClient()
                        .sql(sqlGetPayers)
                        .bind(0, purchaseId)
                        .map((row, meta) -> {
                            FullUser user = new FullUser();
                            user.setLogin(row.get("login", String.class));
                            user.setEmail(row.get("email", String.class));
                            user.setPassword(row.get("password", String.class));
                            user.setName(row.get("name", String.class));
                            user.setSurname(row.get("surname", String.class));
                            user.setCommentMoneyTransfer(row.get("commentMoneyTransfer", String.class));
                            user.setRoleName(null); // роль не используется
                            return user;
                        })
                        .all());
    }

    public Flux<String> getImagePathsByPurchaseId(Integer purchaseId) {
        String sqlGetImagePath = """
                SELECT
                    r.file_path AS file_path
                FROM receipt_list rl
                JOIN receipt r ON r.receipt_id = rl.receipt_id
                WHERE rl.purchase_id = $1
                """;

        return template.getDatabaseClient()
                .sql(sqlGetImagePath)
                .bind(0, purchaseId)
                .map((row, meta) -> row.get("file_path", String.class))
                .all();
    }

    public Flux<Resource> getReceiptResources(Integer eventId, Integer purchaseId) {
        return getImagePathsByPurchaseId(purchaseId)
                .map(path -> resourceLoader.getResource("file:" + path));
    }
}
