package ru.eventorg.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.BadCredentialsException;
import ru.eventorg.exception.ErrorState;

public class SecurityUtils {

    public static Mono<String> getCurrentUserLogin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null || !authentication.isAuthenticated()) {
                        return Mono.error(new BadCredentialsException(ErrorState.BAD_CREDENTIALS));
                    }
                    return Mono.just(authentication.getName());
                });
    }
}