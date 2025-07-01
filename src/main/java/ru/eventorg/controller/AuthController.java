package ru.eventorg.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.UserProfileAuthRequest;
import ru.eventorg.dto.UserProfileRegistrationRequest;
import ru.eventorg.service.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/login")
    public Mono<String> login(@RequestBody UserProfileAuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/register")
    public Mono<String> register(@RequestBody UserProfileRegistrationRequest request) {
        return authService.register(request);
    }
}



