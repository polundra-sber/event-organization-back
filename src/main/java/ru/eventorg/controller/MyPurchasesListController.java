package ru.eventorg.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.MyPurchasesApi;
import org.openapitools.model.EditPurchaseCostInMyPurchasesListRequest;
import org.openapitools.model.GetMyPurchasesList200Response;
import org.openapitools.model.MyPurchaseListItem;
import org.openapitools.model.TaskListItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.MyPurchaseListItemCustom;
import ru.eventorg.dto.MyPurchasesListResponse;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.WrongFileFormatException;
import ru.eventorg.service.MyPurchasesListService;
import ru.eventorg.service.PurchaseValidationService;
import ru.eventorg.service.ReceiptService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MyPurchasesListController implements MyPurchasesApi {
    private final MyPurchasesListService myPurchasesListService;
    private final PurchaseValidationService purchaseValidationService;
    private final ReceiptService receiptService;

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/my-purchases-list/{purchase_id}/add-receipt",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> addReceiptForPurchaseInMyPurchasesList(
            @PathVariable("purchase_id") Integer purchaseId,
            @RequestPart("files") Flux<FilePart> fileParts,
            ServerWebExchange exchange
    ) {
        Flux<FilePart> validated = fileParts
                .handle((part, sink) -> {
                    MediaType ct = part.headers().getContentType();
                    if (MediaType.IMAGE_JPEG.equals(ct) || MediaType.IMAGE_PNG.equals(ct)) {
                        sink.next(part);
                    } else {
                        sink.error(new WrongFileFormatException(ErrorState.WRONG_FILE_FORMAT));
                    }
                });

        return purchaseValidationService.purchaseExists(purchaseId)
                .thenMany(validated)
                .as(fpFlux -> receiptService.storeReceipts(purchaseId, fpFlux))
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> denyPurchaseInMyPurchasesList(Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return myPurchasesListService.denyPurchaseInMyPurchasesList(purchaseId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<Void>> editPurchaseCostInMyPurchasesList(Integer purchaseId, Mono<EditPurchaseCostInMyPurchasesListRequest> editPurchaseCostInMyPurchasesListRequest, ServerWebExchange exchange) throws Exception {
        return myPurchasesListService.editPurchaseCostInMyPurchasesList(purchaseId, editPurchaseCostInMyPurchasesListRequest)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<GetMyPurchasesList200Response>> getMyPurchasesList(ServerWebExchange exchange) throws Exception {
        return myPurchasesListService.getMyPurchasesList()
                .flatMap(response -> {
                    // Преобразуем каждый элемент списка покупок
                    List<MyPurchaseListItem> items = response.getMyPurchasesList().stream()
                            .map(this::convertToMyPurchaseListItem)
                            .collect(Collectors.toList());

                    // Создаем ответ
                    GetMyPurchasesList200Response apiResponse = new GetMyPurchasesList200Response();
                    apiResponse.setUserLogin(response.getUserLogin());
                    apiResponse.setPurchases(items);

                    return Mono.just(ResponseEntity.ok(apiResponse));
                });
    }

    private MyPurchaseListItem convertToMyPurchaseListItem(MyPurchaseListItemCustom customItem) {
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
