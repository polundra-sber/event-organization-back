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
import ru.eventorg.exception.PurchaseNotExistException;
import ru.eventorg.repository.*;

import java.math.BigDecimal;

import static ru.eventorg.security.SecurityUtils.getCurrentUserLogin;

@Service
public class PurchaseListService {
    private final R2dbcEntityTemplate template;
    private final UserProfilesEntityRepository userProfilesEntityRepository;
    private final PurchaseEntityRepository purchaseEntityRepository;
    private final EventValidationService eventValidationService;
    private final RoleService roleService;
    private final ParticipantValidationService participantValidationService;

    public PurchaseListService(R2dbcEntityTemplate template, UserProfilesEntityRepository userProfilesEntityRepository, EventEntityRepository eventEntityRepository, PurchaseEntityRepository purchaseEntityRepository, EventUserListEntityRepository eventUserListEntityRepository, EventValidationService eventValidation, ParticipantValidationService participantValidation, RoleService roleService, ParticipantValidationService participantValidationService) {
        this.template = template;
        this.userProfilesEntityRepository = userProfilesEntityRepository;
        this.purchaseEntityRepository = purchaseEntityRepository;
        this.eventValidationService = eventValidation;
        this.roleService = roleService;
        this.participantValidationService = participantValidationService;
    }

    public Flux<PurchaseWithUserDto> getPurchasesByEventId(Integer eventId) {
        return getCurrentUserLogin()
                .flatMapMany(userLogin ->
                        participantValidationService.validateIsParticipant(eventId, userLogin)
                                .then(eventValidationService.validateExists(eventId))
                                .thenMany(purchaseEntityRepository.findPurchasesWithUserByEventId(eventId))
                                .map(this::mapProjectionToPurchaseWithUserDto)
                                .switchIfEmpty(Flux.just(new PurchaseWithUserDto())));
    }

    public Mono<PurchaseWithUserDto> addPurchaseToPurchasesList(
            Integer eventId,
            Mono<PurchaseListItemCreator> purchaseListItemCreator) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin)
                                .then(eventValidationService.validateExists(eventId))
                                .then(purchaseListItemCreator)
                                .flatMap(creator ->
                                        participantValidationService.validateIsParticipant(eventId, creator.getResponsibleUser())
                                                .then(createAndSavePurchase(eventId, creator))
                                )
                );
    }

    public Mono<Void> deletePurchase(Integer eventId, Integer purchaseId) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin)
                                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId)
                                        .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                        .then(purchaseEntityRepository.deleteByPurchaseIdAndEventId(purchaseId, eventId))
                                )
                );
    }


    public Mono<PurchaseWithUserDto> editPurchaseInPurchasesList(Integer eventId, Integer purchaseId, Mono<PurchaseListItemEditor> purchaseListItemEditor){
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        roleService.checkIfOrganizerOrHigher(eventId, currentUserLogin)
                                .then(eventValidationService.validateExists(eventId))
                                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId))
                                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                .flatMap(existing -> purchaseListItemEditor.flatMap(editor -> {
                                    // Обновляем поля
                                    existing.setPurchaseName(editor.getPurchaseName());
                                    existing.setPurchaseDescription(editor.getPurchaseDescription());
                                    existing.setResponsibleUser(editor.getResponsibleUser());

                                    // Проверяем участника и сохраняем
                                    return participantValidationService.validateIsParticipant(eventId, editor.getResponsibleUser())
                                            .then(purchaseEntityRepository.save(existing))
                                            .flatMap(this::createPurchaseWithUserDto);
                                }))
                );
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


    public Mono<PurchaseWithUserDto> takePurchaseFromPurchasesList(Integer eventId, Integer purchaseId) {
        return getCurrentUserLogin()
                .flatMap(currentUserLogin ->
                        participantValidationService.validateIsParticipant(eventId, currentUserLogin)
                                .then(eventValidationService.validateExists(eventId))
                                .then(purchaseEntityRepository.findByPurchaseIdAndEventId(purchaseId, eventId))
                                .switchIfEmpty(Mono.error(new PurchaseNotExistException(ErrorState.PURCHASE_NOT_EXIST)))
                                .flatMap(purchase -> {
                                    purchase.setResponsibleUser(currentUserLogin);
                                    return purchaseEntityRepository.save(purchase)
                                            .flatMap(this::createPurchaseWithUserDto);
                                })
                );
    }
}
