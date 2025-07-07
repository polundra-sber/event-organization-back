package ru.eventorg.controller;

import org.openapitools.api.StuffListApi;
import org.openapitools.model.StuffListItem;
import org.openapitools.model.StuffListItemCreator;
import org.openapitools.model.StuffListItemEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.StuffWithUserDto;
import ru.eventorg.service.StuffListService;

@RestController
public class StuffListController implements StuffListApi {

    private final StuffListService stuffListService;

    public StuffListController(StuffListService stuffListService) {
        this.stuffListService = stuffListService;
    }

    @Override
    public Mono<ResponseEntity<StuffListItem>> addStuffToStuffsList(Integer eventId, Mono<StuffListItemCreator> stuffListItemCreator, ServerWebExchange exchange) throws Exception {
        return stuffListService.addStuffToStuffsList(eventId, stuffListItemCreator)
                .map(this::convertStuffWithUserDtoToStuffListItem)
                .map(stuffItem -> ResponseEntity.status(HttpStatus.CREATED).body(stuffItem));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteStuffFromStuffsList(Integer eventId, Integer stuffId, ServerWebExchange exchange) throws Exception {
        return stuffListService.deleteStuffFromStuffsList(eventId, stuffId)
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    @Override
    public Mono<ResponseEntity<StuffListItem>> editStuffInStuffsList(Integer eventId, Integer stuffId, Mono<StuffListItemEditor> stuffListItemEditor, ServerWebExchange exchange) throws Exception {
        return stuffListService.editStuffInStuffsList(eventId, stuffId, stuffListItemEditor)
                .map(this::convertStuffWithUserDtoToStuffListItem)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<StuffListItem>>> getStuffsList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return stuffListService.getStuffsList(eventId)
                .map(this::convertStuffWithUserDtoToStuffListItem)
                .collectList()
                .map(list-> ResponseEntity.ok(Flux.fromIterable(list)));
    }

    @Override
    public Mono<ResponseEntity<StuffListItem>> takeStuffFromStuffsList(Integer eventId, Integer stuffId, ServerWebExchange exchange) throws Exception {
        return stuffListService.takeStuffFromStuffsList(eventId, stuffId)
                .map(this::convertStuffWithUserDtoToStuffListItem)
                .map(ResponseEntity::ok);
    }


    //Вспомогательные методы
    private StuffListItem convertStuffWithUserDtoToStuffListItem(StuffWithUserDto stuffWithUserDto) {
        StuffListItem item = new StuffListItem()
                .stuffId(stuffWithUserDto.getStuff().getStuffId())
                .stuffName(stuffWithUserDto.getStuff().getStuffName())
                .stuffDescription(stuffWithUserDto.getStuff().getStuffDescription())
                .responsibleLogin(stuffWithUserDto.getStuff().getResponsibleUser());

        if (stuffWithUserDto.getResponsibleUser() != null) {
            item.responsibleLogin(stuffWithUserDto.getResponsibleUser().getLogin());
            item.responsibleName(stuffWithUserDto.getResponsibleUser().getName());
            item.responsibleSurname(stuffWithUserDto.getResponsibleUser().getSurname());
        }
        return item;
    }
}
