package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.FullUser;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.repository.ReceiptListEntityRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CostListService {
    private final ReceiptListEntityRepository receiptListEntityRepository;
    private final R2dbcEntityTemplate template;
    private final ResourceLoader resourceLoader;

    public Mono<Boolean> hasReceipt(Integer purchaseId) {
        return receiptListEntityRepository.existsReceiptListEntityByPurchaseId(purchaseId);
    }

    public Flux<FullUser> getPayersForPurchase(Integer purchaseId) {
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

        return template.getDatabaseClient()
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
                .all();
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

    public Flux<PurchaseWithUserDto> getPurchasesForUser(Integer eventId, String userLogin) {
        String sqlPurchasesForUser = """
                    SELECT
                        p.purchase_id           AS purchase_id,
                        p.purchase_name         AS purchase_name,
                        p.purchase_description  AS purchase_description,
                        p.cost                  AS cost,
                        p.responsible_user      AS responsible_user,
                        p.event_id              AS event_id,
                        up.login                AS login,
                        up.name                 AS name,
                        up.surname              AS surname,
                        up.comment_money_transfer AS commentMoneyTransfer
                    FROM payer
                    JOIN purchase p ON payer.purchase_id = p.purchase_id
                    JOIN user_profile up ON p.responsible_user = up.login
                    WHERE payer.user_id = $1 AND p.event_id = $2
                    """;
        return template.getDatabaseClient()
                .sql(sqlPurchasesForUser)
                .bind(0, userLogin)
                .bind(1, eventId)
                .map((row, meta) -> {
                    PurchaseEntity purchase = new PurchaseEntity(
                            row.get("purchase_id", Integer.class),
                            row.get("purchase_name", String.class),
                            row.get("purchase_description", String.class),
                            row.get("cost", BigDecimal.class),
                            row.get("responsible_user", String.class),
                            row.get("event_id", Integer.class)
                    );

                    UserProfileEntity user = new UserProfileEntity(
                            row.get("login", String.class),
                            row.get("name", String.class),
                            row.get("surname", String.class),
                            row.get("commentMoneyTransfer", String.class)
                    );

                    return new PurchaseWithUserDto(purchase, user);
                })
                .all();
    }
}
