package ru.eventorg.service;

import org.openapitools.model.PurchaseListItemCreator;
import org.openapitools.model.PurchaseListItemEditor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.exception.EventNotExistException;
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.exception.UserNotEventParticipantException;
import ru.eventorg.repository.*;

import java.math.BigDecimal;

@Service
public class PurchaseListService {
    private final R2dbcEntityTemplate template;
    private final UserProfilesEntityRepository userProfilesEntityRepository;
    private final EventEntityRepository eventEntityRepository;
    private final PurchaseEntityRepository purchaseEntityRepository;
    private final EventUserListEntityRepository eventUserListEntityRepository;

    private static final String GET_PURCHASES_SQL = """
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

    private static final String DELETE_EVENT_SQL = "DELETE FROM purchase WHERE purchase_id = $1 AND event_id = $2";

    public PurchaseListService(R2dbcEntityTemplate template, UserProfilesEntityRepository userProfilesEntityRepository, EventEntityRepository eventEntityRepository, PurchaseEntityRepository purchaseEntityRepository, EventUserListEntityRepository eventUserListEntityRepository) {
        this.template = template;
        this.userProfilesEntityRepository = userProfilesEntityRepository;
        this.eventEntityRepository = eventEntityRepository;
        this.purchaseEntityRepository = purchaseEntityRepository;
        this.eventUserListEntityRepository = eventUserListEntityRepository;
    }


    public Flux<PurchaseWithUserDto> getPurchasesByEventId(Integer eventId) {
        //TODO: проверка по user_id из токена, что просмотреть список хочет участник мероприятия
        return checkEventExists(eventId)
                .flatMapMany(exists -> purchaseEntityRepository.findPurchasesWithUserByEventId(eventId)
                        .map(this::mapProjectionToPurchaseWithUserDto)
                        .switchIfEmpty(Flux.just(new PurchaseWithUserDto())));
    }

    public Mono<PurchaseWithUserDto> addPurchaseToPurchasesList(
            Integer eventId,
            Mono<PurchaseListItemCreator> purchaseListItemCreator) {
        //TODO: проверка, что добавляет организатор или создатель по user_id из токена
        return checkEventExists(eventId)
                .flatMap(exists -> purchaseListItemCreator)
                .flatMap(creator ->
                        validateResponsibleUser(eventId, creator.getResponsibleUser())
                                .then(Mono.defer(() -> {
                                            PurchaseEntity newPurchase = new PurchaseEntity();
                                            newPurchase.setPurchaseName(creator.getPurchaseName());
                                            newPurchase.setPurchaseDescription(creator.getPurchaseDescription());
                                            newPurchase.setCost(BigDecimal.ZERO);
                                            newPurchase.setResponsibleUser(creator.getResponsibleUser());
                                            newPurchase.setEventId(eventId);

                                            return template.insert(PurchaseEntity.class)
                                                    .using(newPurchase)
                                                    .flatMap(this::createPurchaseWithUserDto);
                                        }))
                                );
    }

    public Mono<Void> deletePurchase(Integer eventId, Integer purchaseId) {
        //TODO: проверка, что удаляет организатор или создатель по user_id из токена
        return purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId)
                .switchIfEmpty(Mono.error(new PurchaseNotExistException()))
                .then(purchaseEntityRepository.deleteByPurchaseIdAndEventId(purchaseId, eventId));
    }


    public Mono<PurchaseWithUserDto> editPurchaseInPurchasesList(Integer eventId, Integer purchaseId, Mono<PurchaseListItemEditor> purchaseListItemEditor){
        //TODO: проверка, что редактирует организатор или создатель по user_id из токена
        return checkEventExists(eventId)
                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId))
                .switchIfEmpty(Mono.error(new PurchaseNotExistException()))
                .flatMap(existingPurchase -> purchaseListItemEditor
                        .flatMap(editor -> {
                            if (editor.getPurchaseName() != null) {
                                existingPurchase.setPurchaseName(editor.getPurchaseName());
                            }
                            if (editor.getPurchaseDescription() != null) {
                                existingPurchase.setPurchaseDescription(editor.getPurchaseDescription());
                            }
                            // Проверяем и обновляем ответственного пользователя
                            Mono<Void> validation = editor.getResponsibleUser() != null
                                    ? validateResponsibleUser(eventId, editor.getResponsibleUser())
                                    .doOnSuccess(v -> existingPurchase.setResponsibleUser(editor.getResponsibleUser()))
                                    : Mono.empty();

                            // Сохраняем и возвращаем результат
                            return validation.then(
                                    purchaseEntityRepository.save(existingPurchase)
                                            .flatMap(this::createPurchaseWithUserDto)
                            );
                        })
                );
    }


    // Вспомогательные методы
    private Mono<Boolean> checkEventExists(Integer eventId) {
        return eventEntityRepository.existsEventEntityByEventId(eventId).switchIfEmpty(Mono.error(new EventNotExistException()));
    }


    private PurchaseWithUserDto mapProjectionToPurchaseWithUserDto(PurchaseWithResponsibleUserProjection projection) {
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setPurchaseId(projection.getPurchaseId());
        purchase.setPurchaseName(projection.getPurchaseName());
        purchase.setPurchaseDescription(projection.getPurchaseDescription());
        purchase.setResponsibleUser(projection.getResponsibleUser());
        purchase.setEventId(projection.getEventId());

        UserProfileEntity user = null;
        if (projection.getUserLogin() != null) {
            user = new UserProfileEntity();
            user.setLogin(projection.getUserLogin());
            user.setName(projection.getUserName());
            user.setSurname(projection.getUserSurname());
            user.setCommentMoneyTransfer(projection.getCommentMoneyTransfer());
        }

        return new PurchaseWithUserDto(purchase, user);
    }

    private Mono<PurchaseWithUserDto> createPurchaseWithUserDto(PurchaseEntity purchase) {
        return userProfilesEntityRepository.findByLogin(purchase.getResponsibleUser())
                .map(user -> new PurchaseWithUserDto(purchase, user))
                .defaultIfEmpty(new PurchaseWithUserDto(purchase, null));
    }

    private Mono<Void> validateResponsibleUser(Integer eventId, String userLogin) {
        if (userLogin == null) {
            return Mono.empty(); // Если пользователь не указан - пропускаем проверку
        }

        return eventUserListEntityRepository
                .existsEventIdByEventIdAndUserId(eventId, userLogin)
                .flatMap(isParticipant -> {
                    if (!isParticipant) {
                        return Mono.error(new UserNotEventParticipantException());
                    }
                    return Mono.empty();
                });
    }

}
