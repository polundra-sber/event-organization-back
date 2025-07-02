package ru.eventorg.api;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.HomePageApi;
import org.openapitools.model.AuthSuccessResponse;
import org.openapitools.model.Event;
import org.openapitools.model.EventEditor;
import org.openapitools.model.EventPreview;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.EventStatus;
import ru.eventorg.entity.EventUserList;
import ru.eventorg.model.EventEditor;
import ru.eventorg.model.EventNotExistResponse;
import ru.eventorg.model.EventPreview;
import ru.eventorg.model.EventSecondJoining;
import ru.eventorg.service.HomePageService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class HomePageController implements HomePageApi {

    private final HomePageService homePageService;

    @Override
    public Mono<ResponseEntity<Void>> completeEvent(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.completeEvent(eventId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<Event>> createEvent(Mono<EventEditor> eventEditor, ServerWebExchange exchange) throws Exception {
        return eventEditor.flatMap(e -> homePageService.createEvent(e))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteEvent(Integer eventId, ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.deleteEvent(eventId, exchange);
    }

    @Override
    public Mono<ResponseEntity<EventEditor>> editEvent(Integer eventId, Mono<EventEditor> eventEditor, ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.editEvent(eventId, eventEditor, exchange);
    }

    @Override
    public Mono<ResponseEntity<EventPreview>> findEventById(Integer eventId, ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.findEventById(eventId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Event>> getEventById(Integer eventId, ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.getEventById(eventId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<Event>>> getEvents(ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.getEvents(exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> leaveEvent(Integer eventId, ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.leaveEvent(eventId, exchange);
    }

    @Override
    public Mono<ResponseEntity<String>> sendJoinEventRequest(Integer eventId, ServerWebExchange exchange) throws Exception {
        return HomePageApi.super.sendJoinEventRequest(eventId, exchange);
    }
}