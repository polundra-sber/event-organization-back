package ru.eventorg.service;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyPurchaseListItemCustom;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;

import java.math.BigDecimal;
import java.util.Map;

import static ru.eventorg.security.SecurityUtils.getCurrentUserLogin;

@Service
public class MyPurchasesListService {
    private final DatabaseClient databaseClient;
    private final EventService eventService;
    private final RoleService roleService;

    private static final String GET_MY_PURCHASES_SQL = """
        SELECT p.purchase_id, p.purchase_name, p.purchase_description, p.cost,
               p.responsible_user, p.event_id, e.event_name,
               CASE WHEN EXISTS (
                   SELECT 1 FROM receipt_list rl 
                   WHERE rl.purchase_id = p.purchase_id
               ) THEN TRUE ELSE FALSE END as has_receipt,
               up.name as responsible_name, up.surname as responsible_surname
        FROM purchase p
        JOIN event e ON p.event_id = e.event_id
        JOIN event_user_list eul ON e.event_id = eul.event_id
        JOIN role r ON eul.role_id = r.role_id
        LEFT JOIN user_profile up ON p.responsible_user = up.login
        WHERE eul.user_id = $1
          AND (
              (r.role_name IN ('Создатель', 'Организатор'))
              OR
              (r.role_name = 'Участник' AND p.responsible_user = :userLogin)
          )
    """;


    public MyPurchasesListService(DatabaseClient databaseClient, EventService eventService, RoleService roleService) {
        this.databaseClient = databaseClient;
        this.eventService = eventService;
        this.roleService = roleService;
    }


    public Flux<MyPurchaseListItemCustom> getMyPurchasesList() {
        return getCurrentUserLogin()
                .flatMapMany(userLogin ->
                        databaseClient.sql(GET_MY_PURCHASES_SQL)
                                .bind(0, userLogin)
                                .fetch()
                                .all()
                                .flatMap(this::mapRowToMyPurchaseListItemCustom)
                                .switchIfEmpty(Flux.empty())
                );
    }


    private Mono<MyPurchaseListItemCustom> mapRowToMyPurchaseListItemCustom(Map<String, Object> row) {
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setPurchaseId((Integer) row.get("purchase_id"));
        purchase.setPurchaseName((String) row.get("purchase_name"));
        purchase.setPurchaseDescription((String) row.get("purchase_description"));
        purchase.setCost((BigDecimal) row.get("cost"));
        purchase.setEventId((Integer) row.get("event_id"));

        UserProfileEntity responsibleUser = new UserProfileEntity();
        responsibleUser.setLogin((String) row.get("responsible_user"));
        responsibleUser.setName((String) row.get("responsible_name"));
        responsibleUser.setSurname((String) row.get("responsible_surname"));

        Boolean hasReceipt = (Boolean) row.get("has_receipt");
        String eventName = (String) row.get("event_name");

        return Mono.just(new MyPurchaseListItemCustom(
                purchase,
                responsibleUser,
                hasReceipt,
                eventName
        ));
    }
}

