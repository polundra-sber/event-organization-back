package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserSecret;

public interface UserSecretsRepository extends R2dbcRepository<UserSecret, String> {
    Mono<UserSecret> findByLogin(String login);
    Mono<Boolean> existsByLogin(String login);
    Mono<Boolean> existsByEmail(String email);
}