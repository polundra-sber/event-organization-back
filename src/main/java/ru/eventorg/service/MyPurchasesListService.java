package ru.eventorg.service;

import org.openapitools.model.EditPurchaseCostInMyPurchasesListRequest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyPurchaseListItemCustom;
import ru.eventorg.dto.MyPurchasesListResponse;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.exception.CannotDenyPurchaseException;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.exception.WrongUserRoleException;
import ru.eventorg.repository.PurchaseEntityRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static ru.eventorg.security.SecurityUtils.getCurrentUserLogin;

@Service
public class MyPurchasesListService {
    private final DatabaseClient databaseClient;
    private final EventService eventService;
    private final PurchaseEntityRepository purchaseEntityRepository;
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
    JOIN event_status es ON e.status_id = es.event_status_id
    LEFT JOIN user_profile up ON p.responsible_user = up.login
    WHERE eul.user_id = $1
      AND es.event_status_name = 'активно'
      AND (
          (r.role_name IN ('создатель', 'организатор'))
          OR
          (r.role_name = 'участник' AND p.responsible_user = $1)
      )
    """;

    private static final String DENY_PURCHASE_SQL = """
    UPDATE purchase SET responsible_user = NULL 
    WHERE purchase_id = $1
    AND responsible_user = $2
    AND cost = 0
    RETURNING 1
    """;




    public MyPurchasesListService(DatabaseClient databaseClient, EventService eventService, PurchaseEntityRepository purchaseEntityRepository, RoleService roleService) {
        this.databaseClient = databaseClient;
        this.eventService = eventService;
        this.purchaseEntityRepository = purchaseEntityRepository;
        this.roleService = roleService;
    }


    public Mono<MyPurchasesListResponse> getMyPurchasesList() {
        return getCurrentUserLogin()
                .flatMap(userLogin ->
                        databaseClient.sql(GET_MY_PURCHASES_SQL)
                                .bind(0, userLogin)
                                .fetch()
                                .all()
                                .flatMap(this::mapRowToMyPurchaseListItemCustom)
                                .collectList()
                                .map(purchases -> new MyPurchasesListResponse(userLogin, purchases))
                                .switchIfEmpty(Mono.just(new MyPurchasesListResponse(null, Collections.emptyList()))));
    }

    public Mono<Void> denyPurchaseInMyPurchasesList(Integer purchaseId) {
        return getCurrentUserLogin()
                .flatMap(userLogin ->
                        purchaseEntityRepository.findByPurchaseIdAndResponsibleUser(purchaseId, userLogin)
                                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                .flatMap(purchase -> {
                                    if (purchase.getCost().compareTo(BigDecimal.ZERO) != 0) {
                                        return Mono.error(new CannotDenyPurchaseException(ErrorState.CANNOT_DENY_PURCHASE));
                                    }
                                    return eventService.validateEventIsActive(purchase.getEventId())
                                            .then(
                                                    databaseClient.sql(DENY_PURCHASE_SQL)
                                                            .bind(0, purchaseId)
                                                            .bind(1, userLogin)
                                                            .then()
                                            );
                                })
                );
    }

    public Mono<Void> editPurchaseCostInMyPurchasesList(
            Integer purchaseId,
            Mono<EditPurchaseCostInMyPurchasesListRequest> editPurchaseCostInMyPurchasesListRequest) {

        return getCurrentUserLogin()
                .flatMap(userLogin ->
                        purchaseEntityRepository.findById(purchaseId)
                                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                .flatMap(purchase ->
                                        eventService.validateEventIsActive(purchase.getEventId())
                                                .then(Mono.defer(() ->
                                                        purchaseEntityRepository.existsByPurchaseIdAndResponsibleUser(purchase.getPurchaseId(), userLogin)
                                                                .flatMap(isResponsible -> isResponsible
                                                                        ? Mono.just(true)
                                                                        : roleService.checkIfOrganizerOrHigher(purchase.getEventId(), userLogin)
                                                                        .onErrorReturn(false))
                                                ))
                                                .filter(allowed -> allowed)
                                                .switchIfEmpty(Mono.error(new WrongUserRoleException(ErrorState.NOT_CREATOR_OR_ORGANIZER_ROLE)))
                                                .then(editPurchaseCostInMyPurchasesListRequest
                                                        .map(request -> request.getCost() != null
                                                                ? BigDecimal.valueOf(request.getCost())
                                                                : BigDecimal.ZERO)
                                                        .flatMap(newCost -> {
                                                            purchase.setCost(newCost);
                                                            return purchaseEntityRepository.save(purchase).then();
                                                        })
                                                )
                                )
                );
    }

    // Вспомогательные методы
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

