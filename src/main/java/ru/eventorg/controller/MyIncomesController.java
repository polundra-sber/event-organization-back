package ru.eventorg.controller;

import org.openapitools.api.MyIncomesApi;
import org.openapitools.model.MyIncomeListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MyIncomesController implements MyIncomesApi {
    @Override
    public Mono<ResponseEntity<Flux<MyIncomeListItem>>> getMyIncomesList(ServerWebExchange exchange) throws Exception {
        return MyIncomesApi.super.getMyIncomesList(exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> markIncomeReceivedInMyIncomesList(Integer debtId, ServerWebExchange exchange) throws Exception {
        return MyIncomesApi.super.markIncomeReceivedInMyIncomesList(debtId, exchange);
    }
}
