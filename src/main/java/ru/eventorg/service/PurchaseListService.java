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
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.EventNotExistException;
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.exception.UserNotEventParticipantException;
import ru.eventorg.repository.*;

import java.math.BigDecimal;

@Service
public class PurchaseListService {
    private final R2dbcEntityTemplate template;
    private final UserProfilesEntityRepository userProfilesEntityRepository;
    private final PurchaseEntityRepository purchaseEntityRepository;
    private final EventValidationService eventValidation;
    private final ParticipantValidationService participantValidation;

    public PurchaseListService(R2dbcEntityTemplate template, UserProfilesEntityRepository userProfilesEntityRepository, EventEntityRepository eventEntityRepository, PurchaseEntityRepository purchaseEntityRepository, EventUserListEntityRepository eventUserListEntityRepository, EventValidationService eventValidation, ParticipantValidationService participantValidation) {
        this.template = template;
        this.userProfilesEntityRepository = userProfilesEntityRepository;
        this.purchaseEntityRepository = purchaseEntityRepository;
        this.eventValidation = eventValidation;
        this.participantValidation = participantValidation;
    }

    public Flux<PurchaseWithUserDto> getPurchasesByEventId(Integer eventId) {
        //TODO: проверка по user_id из токена, что просмотреть список хочет участник мероприятия
        return eventValidation.validateExists(eventId)
                .thenMany(purchaseEntityRepository.findPurchasesWithUserByEventId(eventId))
                .map(this::mapProjectionToPurchaseWithUserDto)
                .switchIfEmpty(Flux.just(new PurchaseWithUserDto()));
    }

    public Mono<PurchaseWithUserDto> addPurchaseToPurchasesList(
            Integer eventId,
            Mono<PurchaseListItemCreator> purchaseListItemCreator) {
        //TODO: проверка, что добавляет организатор или создатель по user_id из токена
        return eventValidation.validateExists(eventId)
                .then(purchaseListItemCreator)
                .flatMap(creator ->
                        participantValidation.validateIsParticipant(eventId, creator.getResponsibleUser())
                                .then(createAndSavePurchase(eventId, creator)));
    }

    public Mono<Void> deletePurchase(Integer eventId, Integer purchaseId) {
        //TODO: проверка, что удаляет организатор или создатель по user_id из токена
        return purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId)
                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.STUB)))
                .then(purchaseEntityRepository.deleteByPurchaseIdAndEventId(purchaseId, eventId));
    }


    public Mono<PurchaseWithUserDto> editPurchaseInPurchasesList(Integer eventId, Integer purchaseId, Mono<PurchaseListItemEditor> purchaseListItemEditor){
        //TODO: проверка, что редактирует организатор или создатель по user_id из токена
        return eventValidation.validateExists(eventId)
                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId))
                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.STUB)))
                .flatMap(existing -> purchaseListItemEditor.flatMap(editor -> {
                    // Обновляем поля
                    existing.setPurchaseName(editor.getPurchaseName());
                    existing.setPurchaseDescription(editor.getPurchaseDescription());
                    existing.setResponsibleUser(editor.getResponsibleUser());

                    // Проверяем участника и сохраняем
                    return participantValidation.validateIsParticipant(eventId, editor.getResponsibleUser())
                            .then(purchaseEntityRepository.save(existing))
                            .flatMap(this::createPurchaseWithUserDto);
                }));
    }


    // Вспомогательные методы
    private Mono<PurchaseWithUserDto> createAndSavePurchase(Integer eventId, PurchaseListItemCreator creator) {
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setPurchaseName(creator.getPurchaseName());
        purchase.setPurchaseDescription(creator.getPurchaseDescription());
        purchase.setCost(BigDecimal.ZERO);
        purchase.setResponsibleUser(creator.getResponsibleUser());
        purchase.setEventId(eventId);

        return template.insert(purchase)
                .flatMap(this::createPurchaseWithUserDto);
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
}
