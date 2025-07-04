package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.openapitools.model.User;
import org.openapitools.model.UserLogin;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.security.JwtTokenUtil;
import ru.eventorg.exception.BadCredentialsException;
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public Mono<String> register(User request) {
        return userService.registerUser(request)
                .then(Mono.defer(() -> generateTokenForUser(request.getLogin())));
    }

    public Mono<String> login(UserLogin request) {
        return userDetailsService.findByUsername(request.getLogin())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .switchIfEmpty(Mono.error(new BadCredentialsException(ErrorState.BAD_CREDENTIALS)))
                .flatMap(user -> generateTokenForUser(user.getUsername()));
    }

    private Mono<String> generateTokenForUser(String username) {
        return userDetailsService.findByUsername(username)
                .map(jwtTokenUtil::generateToken);
    }
}
