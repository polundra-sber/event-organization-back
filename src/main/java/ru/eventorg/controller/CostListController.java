package ru.eventorg.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.CostListApi;
import org.openapitools.model.CostAllocationListItem;
import org.openapitools.model.GetCostList200Response;
import org.openapitools.model.ReceiptList;
import org.openapitools.model.UserDemo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.entity.PurchaseEntity;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.service.CostListService;
import ru.eventorg.service.EventService;
import ru.eventorg.service.PurchaseListService;
import ru.eventorg.service.UserService;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CostListController implements CostListApi {
    private final CostListService costListService;
    private final PurchaseListService purchaseListService;
    private final EventService eventService;

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
                            .flatMap(dto -> {
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
                            })
                            .collectList()
                            .map(costItems -> {
                                response.setCostAllocationList(costItems);
                                return ResponseEntity.ok(response);
                            });
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<UserDemo>>> getParticipantsForPurchaseFromCostList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        Flux<UserDemo> userDemoFlux = costListService.getPayersForPurchase(eventId, purchaseId)
                .map(fullUser -> {
                    UserDemo demo = new UserDemo();
                    demo.setLogin(fullUser.getLogin());
                    demo.setEmail(fullUser.getEmail());
                    demo.setName(fullUser.getName());
                    demo.setSurname(fullUser.getSurname());
                    return demo;
                });

        return Mono.just(ResponseEntity.ok(userDemoFlux));
    }

    @Override
    public Mono<ResponseEntity<Flux<CostAllocationListItem>>> getPersonalCostList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return CostListApi.super.getPersonalCostList(eventId, exchange);
    }

    @Override
    public Mono<ResponseEntity<ReceiptList>> getReceiptsForPurchase(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return CostListApi.super.getReceiptsForPurchase(eventId, purchaseId, exchange);
    }
}
