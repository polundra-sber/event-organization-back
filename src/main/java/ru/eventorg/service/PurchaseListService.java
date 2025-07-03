package ru.eventorg.service;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.exception.EventNotExistException;

import java.math.BigDecimal;

@Service
public class PurchaseListService {
    private final R2dbcEntityTemplate template;

    public PurchaseListService(R2dbcEntityTemplate template) {
        this.template = template;
    }

    public Flux<PurchaseWithUserDto> getPurchasesByEventId(Integer eventId) {
        // 1. Сначала проверяем, существует ли мероприятие
        String checkEventSql = "SELECT 1 FROM event WHERE event_id = $1 LIMIT 1";

        return template.getDatabaseClient()
                .sql(checkEventSql)
                .bind(0, eventId)
                .fetch()
                .first() // Проверяем, есть ли хотя бы одна строка
                .flatMapMany(exists -> {
                    // 2. Если мероприятие существует, запрашиваем покупки
                    String sql = """
                        SELECT
                            p.*,
                            up.login AS user_login,
                            up.name AS user_name,
                            up.surname AS user_surname,
                            up.comment_money_transfer AS user_comment
                        FROM purchase p
                        LEFT JOIN user_profile up ON p.responsible_user = up.login
                        WHERE p.event_id = $1
                        """;

                    return template.getDatabaseClient().sql(sql)
                            .bind(0, eventId)
                            .map((row, metadata) -> {
                                PurchaseEntity purchase = new PurchaseEntity();
                                purchase.setPurchaseId(row.get("purchase_id", Integer.class));
                                purchase.setPurchaseName(row.get("purchase_name", String.class));
                                purchase.setPurchaseDescription(row.get("purchase_description", String.class));
                                purchase.setCost(row.get("cost", BigDecimal.class));
                                purchase.setResponsibleUser(row.get("responsible_user", String.class));
                                purchase.setEventId(eventId); // Используем исходный eventId

                                UserProfileEntity user = null;
                                String userLogin = row.get("user_login", String.class);
                                if (userLogin != null) {
                                    user = new UserProfileEntity();
                                    user.setLogin(userLogin);
                                    user.setName(row.get("user_name", String.class));
                                    user.setSurname(row.get("user_surname", String.class));
                                    user.setCommentMoneyTransfer(row.get("user_comment", String.class));
                                }

                                return new PurchaseWithUserDto(purchase, user);
                            })
                            .all()
                            .switchIfEmpty(Flux.just(new PurchaseWithUserDto())); // Пустой DTO, если покупок нет
                })
                .switchIfEmpty(Flux.error(new EventNotExistException())); // Ошибка, если eventId не существует
    }
}
