package ru.eventorg.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserSecretEntity;

public interface UserSecretsEntityRepository extends R2dbcRepository<UserSecretEntity, String> {
    Mono<UserSecretEntity> findByLogin(String login);
    Mono<Boolean> existsByLogin(String login);
    Mono<Boolean> existsByEmail(String email);

    @Modifying
    @Query("UPDATE user_secret SET email = :email WHERE login = :login")
    Mono<Integer> updateEmail(@Param("login") String login, @Param("email") String email);

    @Query("SELECT COUNT(*) > 0 FROM user_secret WHERE email = :email AND login != :login")
    Mono<Boolean> existsByEmailAndLoginNot(@Param("email") String email,
                                           @Param("login") String login);
}