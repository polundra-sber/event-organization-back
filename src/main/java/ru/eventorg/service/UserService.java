package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserProfileEntity;
import ru.eventorg.entity.UserSecretEntity;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.exception.UserAlreadyExistsException;
import ru.eventorg.repository.UserProfilesEntityRepository;
import ru.eventorg.repository.UserSecretsEntityRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfilesEntityRepository profilesRepo;
    private final UserSecretsEntityRepository secretsRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Mono<Void> registerUser(User request) {
        return checkUserExists(request.getLogin(), request.getEmail())
                .then(createAndSaveUser(request));
    }

    private Mono<Void> checkUserExists(String login, String email) {
        return Mono.zip(
                secretsRepo.existsByLogin(login),
                secretsRepo.existsByEmail(email)
        ).flatMap(tuple -> {
            boolean loginExists = tuple.getT1();
            boolean emailExists = tuple.getT2();

            if (loginExists) {
                return Mono.error(new UserAlreadyExistsException(ErrorState.USER_ALREADY_EXISTS));
            }
            if (emailExists) {
                return Mono.error(new UserAlreadyExistsException(ErrorState.USER_ALREADY_EXISTS));
            }
            return Mono.empty();
        });
    }

    private Mono<Void> createAndSaveUser(User request) {
        UserSecretEntity secrets = new UserSecretEntity();
        secrets.setLogin(request.getLogin());
        secrets.setEmail(request.getEmail());
        secrets.setPassword(passwordEncoder.encode(request.getPassword()));

        UserProfileEntity profile = new UserProfileEntity();
        profile.setLogin(request.getLogin());
        profile.setName(request.getName());
        profile.setSurname(request.getSurname());
        profile.setCommentMoneyTransfer(request.getCommentMoneyTransfer());

        return profilesRepo.save(profile)
                .then(secretsRepo.save(secrets))
                .then();
    }
}