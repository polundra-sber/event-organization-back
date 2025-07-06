package ru.eventorg.controller;

import org.openapitools.api.PurchaseListApi;
import org.openapitools.model.PurchaseListItem;
import org.openapitools.model.PurchaseListItemCreator;
import org.openapitools.model.PurchaseListItemEditor;
import org.springframework.http.HttpStatus;
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
    public Mono<ResponseEntity<PurchaseListItem>> addPurchaseToPurchasesList(
            Integer eventId,
            Mono<PurchaseListItemCreator> purchaseListItemCreator,
            ServerWebExchange exchange) {

        return purchaseListService.addPurchaseToPurchasesList(eventId, purchaseListItemCreator)
                .map(this::convertPurchaseWithUserDtoToPurchaseListItem)
                .map(purchaseItem -> ResponseEntity.status(HttpStatus.CREATED).body(purchaseItem));
    }

    @Override
    public Mono<ResponseEntity<Void>> deletePurchaseFromPurchasesList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return purchaseListService.deletePurchase(eventId, purchaseId)
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    @Override
    public Mono<ResponseEntity<PurchaseListItem>> editPurchaseInPurchasesList(Integer eventId, Integer purchaseId, Mono<PurchaseListItemEditor> purchaseListItemEditor, ServerWebExchange exchange) throws Exception {
        return purchaseListService.editPurchaseInPurchasesList(eventId, purchaseId, purchaseListItemEditor)
                .map(this::convertPurchaseWithUserDtoToPurchaseListItem)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PurchaseListItem>>> getPurchasesList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return purchaseListService.getPurchasesByEventId(eventId)
                .map(this::convertPurchaseWithUserDtoToPurchaseListItem)
                .collectList()
                .map(list-> ResponseEntity.ok(Flux.fromIterable(list)));
    }

    private PurchaseListItem convertPurchaseWithUserDtoToPurchaseListItem(PurchaseWithUserDto purchaseWithUserDto) {
        PurchaseListItem item = new PurchaseListItem()
                .purchaseId(purchaseWithUserDto.getPurchase().getPurchaseId())
                .purchaseName(purchaseWithUserDto.getPurchase().getPurchaseName())
                .purchaseDescription(purchaseWithUserDto.getPurchase().getPurchaseDescription())
                .responsibleLogin(purchaseWithUserDto.getPurchase().getResponsibleUser());

        if (purchaseWithUserDto.getResponsibleUser() != null) {
            item.responsibleLogin(purchaseWithUserDto.getResponsibleUser().getLogin());
            item.responsibleName(purchaseWithUserDto.getResponsibleUser().getName());
            item.responsibleSurname(purchaseWithUserDto.getResponsibleUser().getSurname());
        }
        return item;
    }

    @Override
    public Mono<ResponseEntity<PurchaseListItem>> takePurchaseFromPurchasesList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return purchaseListService.takePurchaseFromPurchasesList(eventId, purchaseId)
                .map(this::convertPurchaseWithUserDtoToPurchaseListItem)
                .map(ResponseEntity::ok);
    }
}
