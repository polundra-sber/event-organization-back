package ru.eventorg.service;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.PurchaseListItemCreator;
import org.openapitools.model.PurchaseListItemEditor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.exception.AlreadyHasResponsibleException;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.repository.*;

import java.math.BigDecimal;

import static ru.eventorg.security.SecurityUtils.getCurrentUserLogin;

@Slf4j
@Service
public class PurchaseListService {
    private final R2dbcEntityTemplate template;
    private final UserProfilesEntityRepository userProfilesEntityRepository;
    private final PurchaseEntityRepository purchaseEntityRepository;
    private final DatabaseClient databaseClient;
    private final EventService eventService;
    private final RoleService roleService;
    private static final String GET_PURCHASES_SQL = """
        SELECT
            p.purchase_id, p.purchase_name, p.purchase_description,
            p.responsible_user, p.event_id,
            up.login,
            up.name AS user_name,
            up.surname AS user_surname,
            up.comment_money_transfer AS user_comment
        FROM purchase p
        LEFT JOIN user_profile up ON p.responsible_user = up.login
        WHERE p.event_id = $1
        """;

    public PurchaseListService(R2dbcEntityTemplate template, UserProfilesEntityRepository userProfilesEntityRepository, EventEntityRepository eventEntityRepository, PurchaseEntityRepository purchaseEntityRepository, EventUserListEntityRepository eventUserListEntityRepository, DatabaseClient databaseClient, EventService eventService, RoleService roleService) {
        this.template = template;
        this.userProfilesEntityRepository = userProfilesEntityRepository;
        this.purchaseEntityRepository = purchaseEntityRepository;
        this.databaseClient = databaseClient;
        this.eventService = eventService;
        this.roleService = roleService;
    }


    public Flux<PurchaseWithUserDto> getPurchasesByEventId(Integer eventId) {
        return getCurrentUserLogin()
                .flatMapMany(userLogin ->
                        eventService.validateExists(eventId)
                                .then(roleService.validateIsParticipant(eventId, userLogin))
                                .thenMany(
                                        databaseClient.sql(GET_PURCHASES_SQL)
                                                .bind(0, eventId)
                                                .fetch()
                                                .all()
                                                .flatMap(this::mapRowToPurchaseWithUserDto)
                                )
                                .switchIfEmpty(Flux.just(new PurchaseWithUserDto()))
                );
    }

    public Mono<PurchaseWithUserDto> addPurchaseToPurchasesList(
            Integer eventId,
            Mono<PurchaseListItemCreator> purchaseListItemCreator) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))
                                .then(purchaseListItemCreator)
                                .flatMap(creator ->
                                        roleService.validateIsParticipant(eventId, creator.getResponsibleLogin())
                                                .then(createAndSavePurchase(eventId, creator))
                                )
                );
    }

    public Mono<Void> deletePurchase(Integer eventId, Integer purchaseId) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId)
                                        .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                        .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))
                                        .then(purchaseEntityRepository.deleteByPurchaseIdAndEventId(purchaseId, eventId))
                                )
                );
    }


    public Mono<PurchaseWithUserDto> editPurchaseInPurchasesList(
            Integer eventId,
            Integer purchaseId,
            Mono<PurchaseListItemEditor> purchaseListItemEditor) {

        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId))
                                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                .then(roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin))
                                .then(purchaseListItemEditor.flatMap(editor ->
                                                updateAndSavePurchase(eventId, purchaseId, editor)
                                        )
                                )
                );
    }

    public Mono<PurchaseWithUserDto> takePurchaseFromPurchasesList(Integer eventId, Integer purchaseId) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        eventService.validateExists(eventId)
                                .then(eventService.validateEventIsActive(eventId))
                                .then(roleService.validateIsParticipant(eventId, currentUserLogin))
                                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId))
                                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                .flatMap(purchase -> {
                                    if (purchase.getResponsibleUser() != null) {
                                        return Mono.error(new AlreadyHasResponsibleException(ErrorState.PURCHASE_ALREADY_HAS_RESPONSIBLE));
                                    }
                                    purchase.setResponsibleUser(currentUserLogin);
                                    return purchaseEntityRepository.save(purchase)
                                            .flatMap(this::createPurchaseWithUserDto);
                                })
                );
    }


    // Вспомогательные методы
    private Mono<PurchaseWithUserDto> createAndSavePurchase(Integer eventId, PurchaseListItemCreator creator) {
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setPurchaseName(creator.getPurchaseName());
        purchase.setPurchaseDescription(creator.getPurchaseDescription());
        purchase.setCost(BigDecimal.ZERO);
        purchase.setResponsibleUser(creator.getResponsibleLogin());
        purchase.setEventId(eventId);

        return template.insert(purchase)
                .flatMap(this::createPurchaseWithUserDto);
    }

    private Mono<PurchaseWithUserDto> createPurchaseWithUserDto(PurchaseEntity purchase) {
        return userProfilesEntityRepository.findByLogin(purchase.getResponsibleUser())
                .map(user -> new PurchaseWithUserDto(purchase, user))
                .defaultIfEmpty(new PurchaseWithUserDto(purchase, null));
    }


    private Mono<PurchaseWithUserDto> updateAndSavePurchase(
            Integer eventId,
            Integer purchaseId,
            PurchaseListItemEditor editor) {

        return purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId)
                .flatMap(existing -> {
                    existing.setPurchaseName(editor.getPurchaseName());
                    existing.setPurchaseDescription(editor.getPurchaseDescription());
                    existing.setResponsibleUser(editor.getResponsibleLogin());

                    return roleService.validateIsParticipant(eventId, editor.getResponsibleLogin())
                            .then(purchaseEntityRepository.save(existing))
                            .flatMap(this::createPurchaseWithUserDto);
                });
    }

    private Mono<PurchaseWithUserDto> mapRowToPurchaseWithUserDto(java.util.Map<String, Object> row) {
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setPurchaseId((Integer) row.get("purchase_id"));
        purchase.setPurchaseName((String) row.get("purchase_name"));
        purchase.setPurchaseDescription((String) row.get("purchase_description"));
        purchase.setResponsibleUser((String) row.get("responsible_user"));
        purchase.setEventId((Integer) row.get("event_id"));

        UserProfileEntity user = null;
        if (row.get("login") != null) {
            user = new UserProfileEntity();
            user.setLogin((String) row.get("login"));
            user.setName((String) row.get("user_name"));
            user.setSurname((String) row.get("user_surname"));
            user.setCommentMoneyTransfer((String) row.get("user_comment"));
        }

        return Mono.just(new PurchaseWithUserDto(purchase, user));
    }


}
