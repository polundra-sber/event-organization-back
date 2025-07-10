package ru.eventorg.controller;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.MyIncomesApi;
import org.openapitools.model.MyIncomeListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.DebtIncomesCustom;
import ru.eventorg.service.MyIncomesService;

@RestController
@AllArgsConstructor
public class MyIncomesController implements MyIncomesApi {
    private final MyIncomesService myIncomesService;
    @Override
    public Mono<ResponseEntity<Flux<MyIncomeListItem>>> getMyIncomesList(ServerWebExchange exchange) throws Exception {
        Flux<MyIncomeListItem> debts = myIncomesService.getDebtIncomesCustom()
                .map(this::convertToMyIncomeListItem);
        return Mono.just(ResponseEntity.ok(debts));
    }

    @Override
    public Mono<ResponseEntity<Void>> markIncomeReceivedInMyIncomesList(Integer debtId, ServerWebExchange exchange) throws Exception {
        return myIncomesService.markIncomeReceived(debtId)
                .thenReturn(ResponseEntity.ok().build());
    }

    private MyIncomeListItem convertToMyIncomeListItem(DebtIncomesCustom custom) {
        return new MyIncomeListItem()
                .eventId(custom.getEventId())
                .eventName(custom.getEventName())
                .debtId(custom.getDebtId())
                .payerLogin(custom.getPayerLogin())
                .payerName(custom.getPayerName())
                .payerSurname(custom.getPayerSurname())
                .debtStatusName(custom.getDebtStatusName())
                .debtAmount(custom.getDebtAmount());
    }
}
