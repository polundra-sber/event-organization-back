package ru.eventorg.controller;

import lombok.AllArgsConstructor;
import org.openapitools.api.ProfileApi;
import org.openapitools.model.UserEditor;
import org.openapitools.model.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.UserProfileCustom;
import ru.eventorg.service.ProfileService;

@RestController
@AllArgsConstructor
public class ProfileController implements ProfileApi {
    private final ProfileService profileService;

    @Override
    public Mono<ResponseEntity<UserProfile>> getUserProfile(ServerWebExchange exchange) {
        return profileService.getUserProfileCustom()
                .flatMap(this::convertToUserProfile)
                .map(profile -> ResponseEntity.status(HttpStatus.OK).body(profile));
    }

    @Override
    public Mono<ResponseEntity<UserEditor>> editUserProfile(Mono<UserEditor> userEditor, ServerWebExchange exchange) {
        return userEditor
                .flatMap(editor -> profileService.editUserProfile(editor)
                        .flatMap(this::convertToUserEditor)
                        .map(profile ->ResponseEntity.status(HttpStatus.CREATED).body(profile)));
    }

    public Mono<UserProfile> convertToUserProfile(UserProfileCustom custom) {
        return Mono.just(new UserProfile()
                .login(custom.getLogin())
                .email(custom.getEmail())
                .name(custom.getName())
                .surname(custom.getSurname())
                .commentMoneyTransfer(custom.getCommentMoneyTransfer()));
    }

    public Mono<UserEditor> convertToUserEditor(UserProfileCustom custom) {
        return Mono.just(new UserEditor()
                .email(custom.getEmail())
                .name(custom.getName())
                .surname(custom.getSurname())
                .commentMoneyTransfer(custom.getCommentMoneyTransfer()));
    }
}
