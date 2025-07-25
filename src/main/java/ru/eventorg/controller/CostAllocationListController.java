package ru.eventorg.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.CostAllocationListApi;
import org.openapitools.model.CostAllocationListItem;
import org.openapitools.model.UserDemo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CostAllocationListController implements CostAllocationListApi {
    private final PurchaseListService purchaseListService;
    private final EventService eventService;
    private final CostListService costListService;
    private final RoleService roleService;
    private final EventService eventValidationService;
    private final PurchaseValidationService purchaseValidationService;
    private final CostAllocationListService costAllocationListService;

    @Override
    public Mono<ResponseEntity<Flux<CostAllocationListItem>>> getCostAllocationList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return eventService.validateEventIsActive(eventId)
                .then(SecurityUtils.getCurrentUserLogin())
                .flatMap(login ->
                        roleService.checkIfCreator(eventId, login)
                                .then(
                                        Mono.just(ResponseEntity.ok(
                                                purchaseListService.getPurchasesByEventId(eventId)
                                                .flatMap(dto -> {
                                                    int purchaseId = dto.getPurchase().getPurchaseId();
                                                    return costListService.getPayersForPurchase(purchaseId)
                                                            .count()
                                                            .flatMap(count ->
                                                                    convertToCostAllocationItem(dto, Math.toIntExact(count))
                                                            );
                                                })
                                        ))
                                )
                );
    }

    @Override
    public Mono<ResponseEntity<Flux<UserDemo>>> getParticipantsForPurchaseFromCostAllocationList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return eventValidationService.validateEventIsActive(eventId)
                .then(SecurityUtils.getCurrentUserLogin())
                .flatMap(login ->
                        roleService.checkIfCreator(eventId, login)
                                .then(purchaseValidationService.purchaseInEvent(purchaseId, eventId))
                                .thenReturn(
                                        costListService.getPayersForPurchase(purchaseId)
                                                .map(fullUser -> {
                                                    UserDemo demo = new UserDemo();
                                                    demo.setLogin(fullUser.getLogin());
                                                    demo.setEmail(fullUser.getEmail());
                                                    demo.setName(fullUser.getName());
                                                    demo.setSurname(fullUser.getSurname());
                                                    return demo;
                                                }))
                                        .map(ResponseEntity::ok)
                );
    }

    @Override
    public Mono<ResponseEntity<Void>> sendCostAllocationList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return eventValidationService.validateEventIsActive(eventId)
                .then(SecurityUtils.getCurrentUserLogin())
                .flatMap(login ->
                        roleService.checkIfCreator(eventId, login)
                                .then(purchaseValidationService.allPurchasesHasResponsible(eventId))
                                .then(costAllocationListService.allocateDebtsBetweenParticipants(eventId))
                )
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    /**
     * DELETE /events/{event_id}/cost-allocation-list/{purchase_id}/participants : Удаление участников для данной покупки
     *
     * @param eventId (required)
     * @param purchaseId (required)
     * @param logins (required)
     * @return Участники успешно удалены (status code 200)
     *         or Мероприятие с данным идентификатором не найдено (status code 400)
     */
    @RequestMapping(
            method = RequestMethod.DELETE,
            value = "/events/{event_id}/cost-allocation-list/{purchase_id}/participants",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> deleteParticipantsForPurchaseFromCostAllocationList(
            @Parameter(name = "event_id", required = true, in = ParameterIn.PATH) @PathVariable("event_id") Integer eventId,
            @Parameter(name = "purchase_id", required = true, in = ParameterIn.PATH) @PathVariable("purchase_id") Integer purchaseId,
            @Valid @RequestBody Mono<List<String>> logins,
            ServerWebExchange exchange
    ) {
        return eventValidationService.validateEventIsActive(eventId)
                .then(SecurityUtils.getCurrentUserLogin())
                .flatMap(login ->
                        roleService.checkIfCreator(eventId, login)
                                .then(purchaseValidationService.purchaseInEvent(purchaseId, eventId))
                                .then(costAllocationListService.removeParticipantsFromPurchase(purchaseId, logins))
                )
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    /**
     * POST /events/{event_id}/cost-allocation-list/{purchase_id}/add-participants : Добавить выбранных участников для покупки
     *
     * @param eventId (required)
     * @param purchaseId (required)
     * @param logins (required)
     * @return Участники успешно добавлены (status code 200)
     *         or Мероприятие с данным идентификатором не найдено (status code 400)
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/events/{event_id}/cost-allocation-list/{purchase_id}/add-participants",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> addParticipantsForPurchaseFromCostAllocationList(
            @Parameter(name = "event_id", required = true, in = ParameterIn.PATH) @PathVariable("event_id") Integer eventId,
            @Parameter(name = "purchase_id", required = true, in = ParameterIn.PATH) @PathVariable("purchase_id") Integer purchaseId,
            @Valid @RequestBody Mono<List<String>> logins,
            ServerWebExchange exchange
    ) {
        return eventValidationService.validateEventIsActive(eventId)
                .then(SecurityUtils.getCurrentUserLogin())
                .flatMap(login ->
                        roleService.checkIfCreator(eventId, login)
                                .then(purchaseValidationService.purchaseInEvent(purchaseId, eventId))
                                .then(costAllocationListService.addParticipantsToPurchase(purchaseId, logins))
                )
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    private Mono<CostAllocationListItem> convertToCostAllocationItem(PurchaseWithUserDto dto, Integer countParticipants) {
        PurchaseEntity purchase = dto.getPurchase();
        UserProfileEntity user = dto.getResponsibleUser();

        return costListService.hasReceipt(purchase.getPurchaseId())
                .map(hasReceipt -> {
                    CostAllocationListItem item = new CostAllocationListItem();
                    item.setPurchaseId(purchase.getPurchaseId());
                    item.setPurchaseName(purchase.getPurchaseName());
                    item.setCost(purchase.getCost().floatValue());
                    item.setHasReceipt(hasReceipt);

                    if (user != null) {
                        item.setResponsibleName(user.getName());
                        item.setResponsibleSurname(user.getSurname());
                    }

                    item.setCountParticipants(countParticipants);

                    return item;
                });
    }
}
