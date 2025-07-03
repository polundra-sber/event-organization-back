package ru.eventorg.controller;

import org.openapitools.api.PurchaseListApi;
import org.openapitools.model.PurchaseListItem;
import org.openapitools.model.PurchaseListItemCreator;
import org.openapitools.model.PurchaseListItemEditor;
import org.openapitools.model.PurchaseListItemResponsible;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.PurchaseWithUserDto;
import ru.eventorg.service.PurchaseListService;

@RestController
public class PurchaseListController implements PurchaseListApi {
    private final PurchaseListService purchaseListService;

    public PurchaseListController(PurchaseListService purchaseListService) {
        this.purchaseListService = purchaseListService;
    }

    @Override
    public Mono<ResponseEntity<PurchaseListItem>> addPurchaseToPurchasesList(Integer eventId, Mono<PurchaseListItemCreator> purchaseListItemCreator, ServerWebExchange exchange) throws Exception {
        return PurchaseListApi.super.addPurchaseToPurchasesList(eventId, purchaseListItemCreator, exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> deletePurchaseFromPurchasesList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return PurchaseListApi.super.deletePurchaseFromPurchasesList(eventId, purchaseId, exchange);
    }

    @Override
    public Mono<ResponseEntity<PurchaseListItem>> editPurchaseInPurchasesList(Integer eventId, Integer purchaseId, Mono<PurchaseListItemEditor> purchaseListItemEditor, ServerWebExchange exchange) throws Exception {
        return PurchaseListApi.super.editPurchaseInPurchasesList(eventId, purchaseId, purchaseListItemEditor, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<PurchaseListItem>>> getPurchasesList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return purchaseListService.getPurchasesByEventId(eventId)
                .map(this::convertPurchaseToPurchaseListItem)
                .collectList()
                .map(list-> ResponseEntity.ok(Flux.fromIterable(list)));
    }

    private PurchaseListItem convertPurchaseToPurchaseListItem(PurchaseWithUserDto purchaseWithUserDto) {
        PurchaseListItem item = new PurchaseListItem()
                .purchaseId(purchaseWithUserDto.getPurchase().getPurchaseId())
                .purchaseName(purchaseWithUserDto.getPurchase().getPurchaseName())
                .purchaseDescription(purchaseWithUserDto.getPurchase().getPurchaseDescription())
                .responsibleUser(purchaseWithUserDto.getPurchase().getResponsibleUser());

        if (purchaseWithUserDto.getResponsibleUser() != null) {
            item.responsibleUser(purchaseWithUserDto.getResponsibleUser().getName()+ " " +purchaseWithUserDto.getResponsibleUser().getSurname());
        }
        return item;
    }

    @Override
    public Mono<ResponseEntity<PurchaseListItemResponsible>> takePurchaseFromPurchasesList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return PurchaseListApi.super.takePurchaseFromPurchasesList(eventId, purchaseId, exchange);
    }
}
