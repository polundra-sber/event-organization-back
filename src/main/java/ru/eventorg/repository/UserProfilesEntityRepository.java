package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserProfileEntity;

import java.util.Set;

public interface UserProfilesEntityRepository extends R2dbcRepository<UserProfileEntity, String> {
    Mono<UserProfileEntity> findByLogin(String login);
    Flux<UserProfileEntity> findByLoginIn(Set<String> logins);

    @Modifying
    @Query("UPDATE user_profile SET name = :name, surname = :surname, comment_money_transfer = :comment WHERE login = :login")
    Mono<Integer> updateProfile(@Param("login") String login,
                                @Param("name") String name,
                                @Param("surname") String surname,
                                @Param("comment") String comment);
}
