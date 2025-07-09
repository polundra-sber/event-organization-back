package ru.eventorg.controller;

import org.openapitools.api.MyPurchasesApi;
import org.openapitools.model.EditPurchaseCostInMyPurchasesListRequest;
import org.openapitools.model.MyPurchaseListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyPurchaseListItemCustom;
import ru.eventorg.service.MyPurchasesListService;

import java.util.List;

@RestController
public class MyPurchasesListController implements MyPurchasesApi {
    private final MyPurchasesListService myPurchasesListService;

    public MyPurchasesListController(MyPurchasesListService myPurchasesListService) {
        this.myPurchasesListService = myPurchasesListService;
    }

    @Override
    public Mono<ResponseEntity<Void>> addReceiptForPurchaseInMyPurchasesList(Integer purchaseId, List<Flux<Part>> files, ServerWebExchange exchange) throws Exception {
        return MyPurchasesApi.super.addReceiptForPurchaseInMyPurchasesList(purchaseId, files, exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> denyPurchaseInMyPurchasesList(Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return myPurchasesListService.denyPurchaseInMyPurchasesList(purchaseId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<Void>> editPurchaseCostInMyPurchasesList(Integer purchaseId, Mono<EditPurchaseCostInMyPurchasesListRequest> editPurchaseCostInMyPurchasesListRequest, ServerWebExchange exchange) throws Exception {
        return MyPurchasesApi.super.editPurchaseCostInMyPurchasesList(purchaseId, editPurchaseCostInMyPurchasesListRequest, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<MyPurchaseListItem>>> getMyPurchasesList(ServerWebExchange exchange) throws Exception {
        return myPurchasesListService.getMyPurchasesList()
                .map(this::convertoMyPurchaseListItemCustomToMyPurchaseListItem)
                .collectList()
                .map(list-> ResponseEntity.ok(Flux.fromIterable(list)));
    }

    // Вспомогательные методы
    private MyPurchaseListItem convertoMyPurchaseListItemCustomToMyPurchaseListItem(MyPurchaseListItemCustom customItem) {
        return new MyPurchaseListItem()
                .eventId(customItem.getPurchase().getEventId())
                .eventName(customItem.getEventName())
                .purchaseId(customItem.getPurchase().getPurchaseId())
                .purchaseName(customItem.getPurchase().getPurchaseName())
                .responsibleName(customItem.getResponsibleUser().getName())
                .responsibleSurname(customItem.getResponsibleUser().getSurname())
                .responsibleLogin(customItem.getResponsibleUser().getLogin())
                .cost(customItem.getPurchase().getCost().floatValue())
                .purchaseDescription(customItem.getPurchase().getPurchaseDescription())
                .hasReceipt(customItem.getHasReceipt());
    }


}
