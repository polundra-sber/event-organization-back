package ru.eventorg.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.CostListApi;
import org.openapitools.model.CostAllocationListItem;
import org.openapitools.model.GetCostList200Response;
import org.openapitools.model.ReceiptList;
import org.openapitools.model.UserDemo;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CostListController implements CostListApi {
    private final CostListService costListService;
    private final PurchaseListService purchaseListService;
    private final EventService eventService;
    private final EventService eventValidationService;
    private final PurchaseValidationService purchaseValidationService;
    private final RoleService roleService;

    @Override
    public Mono<ResponseEntity<GetCostList200Response>> getCostList(Integer eventId, ServerWebExchange exchange) throws Exception {
        Mono<List<PurchaseWithUserDto>> purchasesMono = purchaseListService.getPurchasesByEventId(eventId).collectList();
        Mono<Boolean> isCostAllocatedMono = eventService.isCostAllocated(eventId);

        return Mono.zip(purchasesMono, isCostAllocatedMono)
                .flatMap(tuple -> {
                    List<PurchaseWithUserDto> purchases = tuple.getT1();
                    Boolean isCostAllocated = tuple.getT2();

                    GetCostList200Response response = new GetCostList200Response();
                    response.setExpensesExistence(!purchases.isEmpty());

                    if (!Boolean.TRUE.equals(isCostAllocated)) {
                        response.setCostAllocationList(Collections.emptyList());
                        return Mono.just(ResponseEntity.ok(response));
                    }

                    return Flux.fromIterable(purchases)
                            .flatMap(this::convertToCostAllocationItem)
                            .collectList()
                            .map(costItems -> {
                                response.setCostAllocationList(costItems);
                                return ResponseEntity.ok(response);
                            });
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<UserDemo>>> getParticipantsForPurchaseFromCostList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return eventValidationService.validateExists(eventId)
                .then(purchaseValidationService.purchaseInEvent(purchaseId, eventId))
                .then(SecurityUtils.getCurrentUserLogin()
                        .flatMap(login -> roleService.validateIsParticipant(eventId, login)
                                .thenReturn(login)))
                .thenReturn(costListService.getPayersForPurchase(purchaseId))
                .map(flux -> flux.map(fullUser -> {
                    UserDemo demo = new UserDemo();
                    demo.setLogin(fullUser.getLogin());
                    demo.setEmail(fullUser.getEmail());
                    demo.setName(fullUser.getName());
                    demo.setSurname(fullUser.getSurname());
                    return demo;
                }))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<CostAllocationListItem>>> getPersonalCostList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return SecurityUtils.getCurrentUserLogin()
                .flatMapMany(userLogin -> costListService.getPurchasesForUser(eventId, userLogin))
                .flatMap(this::convertToCostAllocationItem)
                .collectList()
                .map(list -> ResponseEntity.ok(Flux.fromIterable(list)));
    }

    @Deprecated
    public Mono<ResponseEntity<ReceiptList>> getReceiptsForPurchase(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return eventValidationService.validateExists(eventId)
                .then(purchaseValidationService.purchaseInEvent(purchaseId, eventId))
                .then(SecurityUtils.getCurrentUserLogin()
                        .flatMap(login ->
                                roleService.validateIsParticipant(eventId, login)
                                        .thenReturn(login)))
                .thenMany(costListService.getReceiptResources(eventId, purchaseId))
                .collectList()
                .flatMap(resources -> {
                    if (resources.isEmpty()) {
                        return Mono.just(ResponseEntity
                                // FIXME ok or not found
                                .status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(new ReceiptList()));
                    }
                    ReceiptList rl = new ReceiptList();
                    rl.setFiles(resources);
                    return Mono.just(ResponseEntity
                            .ok()
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(rl));
                });
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/events/{event_id}/purchases-list/{purchase_id}/get-receipt",
            produces = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public Mono<ResponseEntity<MultiValueMap<String, HttpEntity<Resource>>>> getReceiptImages(
            @Parameter(name = "event_id", required = true, in = ParameterIn.PATH) @PathVariable("event_id") Integer eventId,
            @Parameter(name = "purchase_id", required = true, in = ParameterIn.PATH) @PathVariable("purchase_id") Integer purchaseId
    ) {
        return eventValidationService.validateExists(eventId)
                .then(purchaseValidationService.purchaseInEvent(purchaseId, eventId))
                .then(
                        SecurityUtils.getCurrentUserLogin()
                                .flatMap(login ->
                                        roleService.validateIsParticipant(eventId, login)
                                                .thenReturn(login)
                                )
                )
                .thenMany(costListService.getReceiptResources(eventId, purchaseId))
                .collectList()
                .flatMap(resources -> {
                    if (resources.isEmpty()) {
                        MultipartBodyBuilder builder = new MultipartBodyBuilder();

                        MultiValueMap<String, HttpEntity<Resource>> multipart =
                                (MultiValueMap<String, HttpEntity<Resource>>) (MultiValueMap<?, ?>) builder.build();

                        return Mono.just(ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(multipart));
                    }

                    MultipartBodyBuilder builder = new MultipartBodyBuilder();
                    for (Resource res : resources) {
                        builder.part("file", res)
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "form-data; name=\"file\"; filename=\"" + res.getFilename() + "\"")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE);
                    }

                    MultiValueMap<String, HttpEntity<Resource>> multipart =
                            (MultiValueMap<String, HttpEntity<Resource>>) (MultiValueMap<?, ?>) builder.build();

                    return Mono.just(ResponseEntity.ok()
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(multipart));
                });
    }

    private Mono<CostAllocationListItem> convertToCostAllocationItem(PurchaseWithUserDto dto) {
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

                    return item;
                });
    }
}
