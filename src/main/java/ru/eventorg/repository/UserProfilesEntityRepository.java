package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserProfileEntity;

public interface UserProfilesEntityRepository extends R2dbcRepository<UserProfileEntity, String> {
    Mono<UserProfileEntity> findByLogin(String login);
}
