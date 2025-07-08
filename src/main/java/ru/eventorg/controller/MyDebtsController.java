package ru.eventorg.controller;

import org.openapitools.api.MyDebtsApi;
import org.openapitools.model.MyDebtListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.DebtCustom;
import ru.eventorg.service.MyDebtsService;

public class MyDebtsController implements MyDebtsApi {
    MyDebtsService myDebtsService;
    @Override
    public Mono<ResponseEntity<Flux<MyDebtListItem>>> getMyDebtsList(ServerWebExchange exchange) throws Exception {
        Flux<MyDebtListItem> debts = myDebtsService.getDebtCustom()
                .map(this::convertToMyDebtListItem);
        return Mono.just(ResponseEntity.ok(debts));
    }

    @Override
    public Mono<ResponseEntity<Void>> markDebtPaidInMyDebtsList(Integer debtId, ServerWebExchange exchange) throws Exception {
        return myDebtsService.markDebtPaid(debtId)
                .thenReturn(ResponseEntity.ok().build());
    }

    private MyDebtListItem convertToMyDebtListItem(DebtCustom custom) {
        return new MyDebtListItem()
                .eventId(custom.getEventId())
                .eventName(custom.getEventName())
                .debtId(custom.getDebtId())
                .recipientLogin(custom.getRecipientLogin())
                .recipientName(custom.getRecipientName())
                .recipientSurname(custom.getRecipientSurname())
                .debtStatusName(custom.getDebtStatusName())
                .debtAmount(custom.getDebtAmount());
    }
}
