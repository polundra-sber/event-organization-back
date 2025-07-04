package ru.eventorg.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.ParticipantsListApi;
import org.openapitools.model.User;
import org.openapitools.model.UserDemo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.ParticipantsListService;
import ru.eventorg.service.RoleService;
import ru.eventorg.service.UserService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ParticipantsListController implements ParticipantsListApi {

    private final ParticipantsListService participantsListService;
    private final RoleService roleService;

    // TODO переделать сигнатуру метода и избавиться от override
    @Override
    public Mono<ResponseEntity<Void>> addParticipants(Integer eventId, Flux<String> requestBody, ServerWebExchange exchange) throws Exception {
        Flux<String> loggedRequestBody = requestBody
                .doOnNext(login -> log.info("Received login: {}", login));
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login ->
                        roleService.checkIfCreator(eventId, login)
                                .flatMap(isCreator -> participantsListService.addParticipantsToEvent(eventId, loggedRequestBody)
                                        .then(Mono.just(ResponseEntity.ok().<Void>build())))
                );
    }

    @Override
    public Mono<ResponseEntity<Flux<User>>> getEventParticipantsList(Integer eventId, ServerWebExchange exchange) throws Exception {
        Flux<User> result = participantsListService.getParticipantsById(eventId)
                .map(fullUser -> {
                    User dto = new User();
                    dto.setLogin(fullUser.getLogin());
                    dto.setRoleId(fullUser.getRoleId());
                    dto.setEmail(fullUser.getEmail());
                    dto.setName(fullUser.getName());
                    dto.setSurname(fullUser.getSurname());
                    dto.setCommentMoneyTransfer(fullUser.getCommentMoneyTransfer());
                    return dto;
                });

        return Mono.just(ResponseEntity.ok(result));
    }

    @Override
    public Mono<ResponseEntity<Flux<UserDemo>>> searchUsers(Integer eventId, String text, Integer seq, ServerWebExchange exchange) throws Exception {
        return ParticipantsListApi.super.searchUsers(eventId, text, seq, exchange);
    }
}
