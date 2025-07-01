package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.pojos.UserSecrets;

public interface UserSecretsRepository extends R2dbcRepository<UserSecrets, String> {
    Mono<UserSecrets> findByLogin(String login);
    Mono<Boolean> existsByLogin(String login);
    Mono<Boolean> existsByEmail(String email);
}