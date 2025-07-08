package ru.eventorg.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import org.openapitools.api.CostAllocationListApi;
import org.openapitools.model.CostAllocationListItem;
import org.openapitools.model.UserDemo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class CostAllocationListController implements CostAllocationListApi {
    @Override
    public Mono<ResponseEntity<Flux<CostAllocationListItem>>> getCostAllocationList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return CostAllocationListApi.super.getCostAllocationList(eventId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<UserDemo>>> getParticipantsForPurchaseFromCostAllocationList(Integer eventId, Integer purchaseId, ServerWebExchange exchange) throws Exception {
        return CostAllocationListApi.super.getParticipantsForPurchaseFromCostAllocationList(eventId, purchaseId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> sendCostAllocationList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return CostAllocationListApi.super.sendCostAllocationList(eventId, exchange);
    }

    /**
     * DELETE /events/{event_id}/cost-allocation-list/{purchase_id}/participants : Удаление участников для данной покупки
     *
     * @param eventId (required)
     * @param purchaseId (required)
     * @param logins (required)
     * @return Участники успешно удалены (status code 200)
     *         or Мероприятие с данным идентификатором не найдено (status code 400)
     */
    @RequestMapping(
            method = RequestMethod.DELETE,
            value = "/events/{event_id}/cost-allocation-list/{purchase_id}/participants",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> deleteParticipantsForPurchaseFromCostAllocationList(
            @Parameter(name = "event_id", required = true, in = ParameterIn.PATH) @PathVariable("event_id") Integer eventId,
            @Parameter(name = "purchase_id", required = true, in = ParameterIn.PATH) @PathVariable("purchase_id") Integer purchaseId,
            @Valid @RequestBody Mono<List<String>> logins,
            ServerWebExchange exchange
    ) {
        return Mono.empty();
    }

    /**
     * POST /events/{event_id}/cost-allocation-list/{purchase_id}/add-participants : Добавить выбранных участников для покупки
     *
     * @param eventId (required)
     * @param purchaseId (required)
     * @param logins (required)
     * @return Участники успешно добавлены (status code 200)
     *         or Мероприятие с данным идентификатором не найдено (status code 400)
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/events/{event_id}/cost-allocation-list/{purchase_id}/add-participants",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> addParticipantsForPurchaseFromCostAllocationList(
            @Parameter(name = "event_id", required = true, in = ParameterIn.PATH) @PathVariable("event_id") Integer eventId,
            @Parameter(name = "purchase_id", required = true, in = ParameterIn.PATH) @PathVariable("purchase_id") Integer purchaseId,
            @Valid @RequestBody Mono<List<String>> logins,
            ServerWebExchange exchange
    ) {
        return Mono.empty();
    }
}
