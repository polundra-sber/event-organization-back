package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserProfile;

public interface UserProfilesRepository extends R2dbcRepository<UserProfile, String> {
    Mono<UserProfile> findByLogin(String login);
}
