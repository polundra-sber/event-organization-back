package ru.eventorg.controller;

import lombok.RequiredArgsConstructor;
import org.openapitools.api.UserMetadataApi;
import org.openapitools.model.EventUserMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.eventorg.security.SecurityUtils;
import ru.eventorg.service.EventService;
import ru.eventorg.service.RoleService;

@RestController
@RequiredArgsConstructor
public class UserMetadataController implements UserMetadataApi {
    private final RoleService roleService;
    private final EventService eventService;

    @Override
    public Mono<ResponseEntity<EventUserMetadata>> getUserMetadata(Integer eventId, ServerWebExchange exchange) throws Exception {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login ->
                        roleService.validateIsParticipant(eventId, login)
                                .then(
                                        Mono.zip(
                                                roleService.getUserRoleInEvent(eventId, login),
                                                eventService.getEventStatus(eventId),
                                                (roleName, eventStatus) -> {
                                                    EventUserMetadata meta = new EventUserMetadata();
                                                    meta.setRoleName(roleName);
                                                    meta.setEventStatusName(eventStatus);
                                                    return ResponseEntity.ok(meta);
                                                }
                                        )
                                )
                );
    }
}
