package ru.eventorg.service;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.AllArgsConstructor;
import org.openapitools.model.UserEditor;
import org.openapitools.model.UserProfile;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.UserProfileCustom;
import ru.eventorg.exception.EmailAlreadyExistsException;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.repository.UserProfilesEntityRepository;
import ru.eventorg.repository.UserSecretsEntityRepository;
import ru.eventorg.security.SecurityUtils;

@Service
@AllArgsConstructor
public class ProfileService {
    private final R2dbcEntityTemplate template;
    private final UserSecretsEntityRepository userSecretsEntityRepository;
    private final UserProfilesEntityRepository userProfilesEntityRepository;

    public Mono<UserProfileCustom> getUserProfileCustom() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login -> {
                    String query = """
                        SELECT 
                            up.login,
                            up.name,
                            up.surname,
                            up.comment_money_transfer,
                            us.email
                        FROM user_profile up
                        JOIN user_secret us ON up.login = us.login
                        WHERE up.login = :login
                        """;

                    return template.getDatabaseClient()
                            .sql(query)
                            .bind("login", login)
                            .map(this::mapRowToUserProfileCustom)
                            .one();
                });
    }

    private UserProfileCustom mapRowToUserProfileCustom(Row row, RowMetadata metadata) {
        return new UserProfileCustom(
                row.get("login", String.class),
                row.get("email", String.class),
                row.get("name", String.class),
                row.get("surname", String.class),
                row.get("comment_money_transfer", String.class)
        );
    }

    public Mono<UserProfileCustom> editUserProfile(UserEditor editor) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(login -> {
                    // Проверяем, что email не занят другим пользователем
                    return userSecretsEntityRepository.existsByEmailAndLoginNot(editor.getEmail(), login)
                            .flatMap(emailExists -> {
                                if (emailExists) {
                                    return Mono.error(new EmailAlreadyExistsException(ErrorState.EMAIL_ALREADY_EXISTS));
                                }
                                return performUpdates(login, editor);
                            });
                });
    }

    private Mono<UserProfileCustom> performUpdates(String login, UserEditor editor) {
        Mono<Void> updateProfile = userProfilesEntityRepository.updateProfile(
                login,
                editor.getName(),
                editor.getSurname(),
                editor.getCommentMoneyTransfer()
        ).then();

        Mono<Void> updateEmail = userSecretsEntityRepository.updateEmail(login, editor.getEmail()).then();

        return Mono.when(updateProfile, updateEmail)
                .thenReturn(new UserProfileCustom(
                        login,
                        editor.getEmail(),
                        editor.getName(),
                        editor.getSurname(),
                        editor.getCommentMoneyTransfer()
                ));
    }
}
