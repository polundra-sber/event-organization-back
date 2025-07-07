package ru.eventorg.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.HomePageApi;
import org.openapitools.model.Event;
import org.openapitools.model.EventEditor;
import org.openapitools.model.EventPreview;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.EventCustom;
import ru.eventorg.dto.EventPreviewCustom;
import ru.eventorg.entity.EventEntity;
import ru.eventorg.service.HomePageService;

@RestController
@RequiredArgsConstructor
public class HomePageController implements HomePageApi {

    private final HomePageService homePageService;

    @Override
    public Mono<ResponseEntity<Void>> completeEvent(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.completeEvent(eventId)
                .thenReturn(ResponseEntity.status(HttpStatus.OK).build());
    }

    @Override
    public Mono<ResponseEntity<Event>> createEvent(Mono<EventEditor> eventEditor, ServerWebExchange exchange) throws Exception {
        return eventEditor
                .flatMap(homePageService::createEvent)
                .map(this::toEventModel)  // Преобразование Entity в DTO
                .map(event -> ResponseEntity.status(HttpStatus.CREATED).body(event));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteEvent(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.deleteEvent(eventId)
                .thenReturn(ResponseEntity.status(HttpStatus.OK).build());
    }


    @Override
    public Mono<ResponseEntity<EventEditor>> editEvent(Integer eventId, Mono<EventEditor> eventEditor, ServerWebExchange exchange) throws Exception {
        return eventEditor.flatMap(editor -> homePageService.editEvent(eventId, editor)
                .flatMap(event -> {
                    EventEditor editorModel = toEventEditorModel(event);
                    return Mono.just(ResponseEntity.status(HttpStatus.OK).body(editorModel));
                }));
    }

    @Override
    public Mono<ResponseEntity<EventPreview>> findEventById(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.findEventById(eventId)
                .flatMap(event -> {
                    EventPreview preview = toEventPreviewModel(event);
                    return Mono.just(ResponseEntity.status(HttpStatus.OK).body(preview));
                });
    }

    @Override
    public Mono<ResponseEntity<Event>> getEventById(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.getEventById(eventId)
                .map(this::toEventModel)  // Преобразование Entity в DTO
                .map(event -> ResponseEntity.status(HttpStatus.OK).body(event));
    }

    @Override
    public Mono<ResponseEntity<Flux<Event>>> getEvents(ServerWebExchange exchange) throws Exception {
        Flux<Event> events = homePageService.getEvents()
                .map(this::toEventModel); // Преобразуем каждый EventEntity в Event
        return Mono.just(ResponseEntity.ok(events));
    }

    @Override
    public Mono<ResponseEntity<Void>> leaveEvent(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.leaveEvent(eventId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<String>> sendJoinEventRequest(Integer eventId, ServerWebExchange exchange) throws Exception {
        return homePageService.sendJoinEventRequest(eventId)
                .map(ResponseEntity::ok);
    }

    public Event toEventModel(EventCustom entity) {
        Event apiEvent = new Event();
        apiEvent.setEventId(entity.getEventId());
        apiEvent.setEventName(entity.getEventName());
        apiEvent.setEventDescription(entity.getEventDescription());
        apiEvent.setEventStatusName(entity.getStatus());
        apiEvent.setLocation(entity.getLocation());
        apiEvent.setEventDate(entity.getEventDate());
        apiEvent.setEventTime(entity.getEventTime() != null ? entity.getEventTime().toString() : null);
        apiEvent.setChatLink(entity.getChatLink());
        apiEvent.setRoleName(entity.getRole());
        return apiEvent;
    }

    public EventPreview toEventPreviewModel(EventPreviewCustom entity) {
        EventPreview preview = new EventPreview();
        preview.setEventId(entity.getEventId());
        preview.setEventName(entity.getEventName());
        preview.setEventStatusName(entity.getStatus());
        preview.setLocation(entity.getLocation());
        preview.setEventDate(entity.getEventDate());
        preview.setEventTime(entity.getEventTime().toString());
        return preview;
    }

    public EventEditor toEventEditorModel(EventEntity entity) {
        EventEditor editor = new EventEditor();
        editor.setEventName(entity.getEventName());
        editor.setEventDescription(entity.getEventDescription());
        editor.setLocation(entity.getLocation());
        editor.setEventDate(entity.getEventDate());
        editor.setEventTime(entity.getEventTime() != null ? entity.getEventTime().toString() : null);
        editor.setChatLink(entity.getChatLink());
        return editor;
    }
}