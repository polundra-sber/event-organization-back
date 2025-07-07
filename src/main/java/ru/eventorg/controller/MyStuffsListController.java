package ru.eventorg.controller;

import org.openapitools.api.MyStuffsApi;
import org.openapitools.model.MyStuffListItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.StuffWithEventDto;
import ru.eventorg.service.MyStuffsListService;

@RestController
public class MyStuffsListController implements MyStuffsApi {
    private final MyStuffsListService myStuffsListService;

    public MyStuffsListController(MyStuffsListService myStuffsListService) {
        this.myStuffsListService = myStuffsListService;
    }

    @Override
    public Mono<ResponseEntity<Void>> denyStuffInMyStuffsList(Integer eventId, Integer stuffId, ServerWebExchange exchange) throws Exception {
        return myStuffsListService.denyStuffInMyStuffsList(eventId, stuffId)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<MyStuffListItem>>> getMyStuffsList(ServerWebExchange exchange) throws Exception {
        return myStuffsListService.getMyStuffsList()
                .map(this::convertStuffWithEventDtoToMyStuffListItem)
                .collectList()
                .map(list-> ResponseEntity.ok(Flux.fromIterable(list)));
    }


    //Вспомогательные методы
    private MyStuffListItem convertStuffWithEventDtoToMyStuffListItem(StuffWithEventDto stuffWithEventDto) {
        return new MyStuffListItem()
                .stuffId(stuffWithEventDto.getStuff().getStuffId())
                .stuffName(stuffWithEventDto.getStuff().getStuffName())
                .stuffDescription(stuffWithEventDto.getStuff().getStuffDescription())
                .eventId(stuffWithEventDto.getEventId())
                .eventName(stuffWithEventDto.getEventName());
    }
}
