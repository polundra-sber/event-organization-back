package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserSecretEntity;

public interface UserSecretsEntityRepository extends R2dbcRepository<UserSecretEntity, String> {
    Mono<UserSecretEntity> findByLogin(String login);
    Mono<Boolean> existsByLogin(String login);
    Mono<Boolean> existsByEmail(String email);
}