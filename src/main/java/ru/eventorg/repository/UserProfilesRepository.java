package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.eventorg.pojos.UserProfiles;

public interface UserProfilesRepository extends R2dbcRepository<UserProfiles, String> {
    Mono<UserProfiles> findByLogin(String login);
}
