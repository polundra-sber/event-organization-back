package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.UserProfileRegistrationRequest;
import ru.eventorg.exception.UserExistsException;
import ru.eventorg.pojos.UserProfiles;
import ru.eventorg.pojos.UserSecrets;
import ru.eventorg.repository.UserProfilesRepository;
import ru.eventorg.repository.UserSecretsRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfilesRepository profilesRepo;
    private final UserSecretsRepository secretsRepo;
    private final PasswordEncoder passwordEncoder;

    public Mono<Void> registerUser(UserProfileRegistrationRequest request) {
        return checkUserExists(request.login(), request.email())
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
                return Mono.error(new UserExistsException("Login already taken"));
            }
            if (emailExists) {
                return Mono.error(new UserExistsException("Email already registered"));
            }
            return Mono.empty();
        });
    }

    private Mono<Void> createAndSaveUser(UserProfileRegistrationRequest request) {
        UserSecrets secrets = new UserSecrets();
        secrets.setLogin(request.login());
        secrets.setEmail(request.email());
        secrets.setPassword(passwordEncoder.encode(request.password()));

        UserProfiles profile = new UserProfiles();
        profile.setLogin(request.login());
        profile.setName(request.name());
        profile.setSurname(request.surname());
        profile.setCommentMoneyTransfer(request.commentMoneyTransfer());

        return profilesRepo.save(profile)
                .then(secretsRepo.save(secrets))
                .then();
    }
}