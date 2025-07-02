package ru.eventorg.controller;

import org.openapitools.api.AuthenticationApi;
import org.openapitools.model.AuthSuccessResponse;
import org.openapitools.model.User;
import org.openapitools.model.UserLogin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.eventorg.service.AuthService;

@RestController
public class AuthenticationController implements AuthenticationApi {

    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Mono<ResponseEntity<AuthSuccessResponse>> createUser(Mono<User> user, ServerWebExchange exchange) throws Exception {
        return user
                .flatMap(userMap ->
                        authService.register(userMap)
                                .map(token -> {
                                    AuthSuccessResponse resp = new AuthSuccessResponse().token(token);
                                    return ResponseEntity.status(HttpStatus.CREATED).body(resp);
                                })
                );
    }

    @Override
    public Mono<ResponseEntity<AuthSuccessResponse>> login(Mono<UserLogin> userLogin, ServerWebExchange exchange) throws Exception {
        return userLogin
                .flatMap(credentials ->
                        authService.login(credentials)
                                .map(token -> {
                                    AuthSuccessResponse resp = new AuthSuccessResponse().token(token);
                                    return ResponseEntity
                                            .status(HttpStatus.CREATED)
                                            .body(resp);
                                })
                );
    }
}
