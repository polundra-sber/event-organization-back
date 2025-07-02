package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.eventorg.entity.UserProfile;
import ru.eventorg.entity.UserSecret;
import ru.eventorg.exception.UserAlreadyExistsException;
import ru.eventorg.repository.UserProfilesRepository;
import ru.eventorg.repository.UserSecretsRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfilesRepository profilesRepo;
    private final UserSecretsRepository secretsRepo;
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
                return Mono.error(new UserAlreadyExistsException("Login already taken"));
            }
            if (emailExists) {
                return Mono.error(new UserAlreadyExistsException("Email already registered"));
            }
            return Mono.empty();
        });
    }

    private Mono<Void> createAndSaveUser(User request) {
        UserSecret secrets = new UserSecret();
        secrets.setLogin(request.getLogin());
        secrets.setEmail(request.getEmail());
        secrets.setPassword(passwordEncoder.encode(request.getPassword()));

        UserProfile profile = new UserProfile();
        profile.setLogin(request.getLogin());
        profile.setName(request.getName());
        profile.setSurname(request.getSurname());
        profile.setCommentMoneyTransfer(request.getCommentMoneyTransfer());

        return profilesRepo.save(profile)
                .then(secretsRepo.save(secrets))
                .then();
    }
}