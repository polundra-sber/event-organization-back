package ru.eventorg.controller;

import org.openapitools.api.MyTasksApi;
import org.openapitools.model.MyTaskListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MyTasksController implements MyTasksApi {

    @Override
    public Mono<ResponseEntity<Void>> denyTaskInMyTasksList(Integer eventId, Integer taskId, ServerWebExchange exchange) throws Exception {
        return MyTasksApi.super.denyTaskInMyTasksList(eventId, taskId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<MyTaskListItem>>> getMyTasksList(ServerWebExchange exchange) throws Exception {
        return MyTasksApi.super.getMyTasksList(exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> markTaskCompletedInMyTasksList(Integer eventId, Integer taskId, ServerWebExchange exchange) throws Exception {
        return MyTasksApi.super.markTaskCompletedInMyTasksList(eventId, taskId, exchange);
    }
}
