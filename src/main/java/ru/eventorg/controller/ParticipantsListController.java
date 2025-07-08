package ru.eventorg.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.ParticipantsListApi;
import org.openapitools.model.User;
import org.openapitools.model.UserDemo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.ParticipantsListService;
import ru.eventorg.service.RoleService;
import ru.eventorg.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ParticipantsListController implements ParticipantsListApi {

    private final ParticipantsListService participantsListService;
    private final RoleService roleService;
    private final EventService eventValidationService;

    /**
     * POST /events/{event_id}/participants-list/add-participant : Добавить выбранных участников
     *
     * @param eventId  (required)
     * @param requestBody (required)
     * @return Участники успешно добавлены (status code 200)
     *         or Неверный формат данных или некорректные логины (status code 400)
     *         or Событие не найдено (status code 404)
     */
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/events/{event_id}/participants-list/add-participant",
            produces = { "application/json" }
    )
    public Mono<ResponseEntity<Void>> addParticipants(
            @Parameter(name = "event_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("event_id") Integer eventId,
            @Valid @RequestBody Mono<List<String>> requestBody,
            @Parameter(hidden = true) final ServerWebExchange exchange) throws Exception {
        return eventValidationService.validateExists(eventId)
                .then(SecurityUtils.getCurrentUserLogin()
                        .flatMap(login ->
                                roleService.checkIfCreator(eventId, login)
                                        .flatMap(isCreator ->
                                                participantsListService.addParticipantsToEvent(eventId, requestBody)
                                                        .then(Mono.just(ResponseEntity.ok().<Void>build()))
                                        )
                        )
                );
    }

    @Override
    public Mono<ResponseEntity<Flux<User>>> getEventParticipantsList(Integer eventId, ServerWebExchange exchange) throws Exception {
        return eventValidationService.validateExists(eventId)
                .then(SecurityUtils.getCurrentUserLogin()
                        .flatMap(login ->
                                roleService.validateIsParticipant(eventId, login)
                                        .thenReturn(login)
                        )
                )
                .flatMap(validatedLogin -> {
                    Flux<User> result = participantsListService.getParticipantsById(eventId)
                            .map(fullUser -> {
                                User dto = new User();
                                dto.setLogin(fullUser.getLogin());
                                dto.setRoleName(fullUser.getRoleName());
                                dto.setEmail(fullUser.getEmail());
                                dto.setName(fullUser.getName());
                                dto.setSurname(fullUser.getSurname());
                                dto.setCommentMoneyTransfer(fullUser.getCommentMoneyTransfer());
                                return dto;
                            });

                    return Mono.just(ResponseEntity.ok(result));
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<UserDemo>>> searchUsers(Integer eventId, String text, Integer seq, ServerWebExchange exchange) throws Exception {
        if (seq == null || seq < 0) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (text == null || text.isBlank()) {
            return Mono.just(ResponseEntity.ok(Flux.empty()));
        }

        return eventValidationService.validateExists(eventId)
                .then(SecurityUtils.getCurrentUserLogin()
                        .flatMap(login ->
                                roleService.validateIsParticipant(eventId, login)
                                        .thenReturn(login)
                        )
                        .flatMap(validatedLogin -> {
                            Flux<UserDemo> result = participantsListService
                                    .searchUsersByNameSurnameEmail(eventId, text, seq)
                                    .map(fullUser -> {
                                        UserDemo dto = new UserDemo();
                                        dto.setLogin(fullUser.getLogin());
                                        dto.setEmail(fullUser.getEmail());
                                        dto.setName(fullUser.getName());
                                        dto.setSurname(fullUser.getSurname());
                                        return dto;
                                    });

                            return result
                                    .hasElements()
                                    .map(hasElements -> ResponseEntity.ok(result));
                        }));
    }
}
