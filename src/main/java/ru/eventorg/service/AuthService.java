package ru.eventorg.service;

import org.openapitools.model.User;
import org.openapitools.model.UserLogin;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {
    public Mono<String> register(User user) {
        return Mono.just("Token");
    }

    public Mono<String> login(UserLogin credentials) {
        return Mono.just("Token");
    }
}
