package ru.eventorg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.eventorg.dto.UserProfileAuthRequest;
import ru.eventorg.dto.UserProfileRegistrationRequest;
import ru.eventorg.security.JwtTokenUtil;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public Mono<String> register(User request) {
        return userService.registerUser(request)
                .then(Mono.defer(() -> generateTokenForUser(request.login())));
    }

    public Mono<String> login(UserProfileAuthRequest request) {
        return userDetailsService.findByUsername(request.login())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid credentials")))
                .flatMap(user -> generateTokenForUser(user.getUsername()));
    }

    private Mono<String> generateTokenForUser(String username) {
        return userDetailsService.findByUsername(username)
                .map(jwtTokenUtil::generateToken);
    }
}
