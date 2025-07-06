package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserProfileEntity;

import java.util.Set;

public interface UserProfilesEntityRepository extends R2dbcRepository<UserProfileEntity, String> {
    Mono<UserProfileEntity> findByLogin(String login);
    Flux<UserProfileEntity> findByLoginIn(Set<String> logins);
}
