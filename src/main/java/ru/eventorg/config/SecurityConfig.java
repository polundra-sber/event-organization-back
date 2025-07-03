package ru.eventorg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;
import ru.eventorg.repository.UserSecretsEntityRepository;
import ru.eventorg.security.JwtTokenUtil;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager authenticationManager,
            JwtTokenUtil jwtTokenUtil
    ) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(authenticationManager);
        jwtFilter.setServerAuthenticationConverter(jwtAuthenticationConverter(jwtTokenUtil));

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/login", "/auth/register").permitAll()  // Разрешаем доступ без авторизации
                        .anyExchange().authenticated()           // Все остальные пути требуют авторизации
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    // Конвертер для извлечения JWT из запроса
    private ServerAuthenticationConverter jwtAuthenticationConverter(JwtTokenUtil jwtTokenUtil) {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                return jwtTokenUtil.validateToken(token)
                        .flatMap(valid -> {
                            if (!valid) {
                                return Mono.error(new BadCredentialsException("Invalid token"));
                            }

                            try {
                                String username = jwtTokenUtil.extractUsername(token);
                                return Mono.just(new UsernamePasswordAuthenticationToken(username, null));
                            } catch (Exception e) {
                                return Mono.error(new BadCredentialsException("Invalid token"));
                            }
                        });
            }
            return Mono.empty();
        };
    }

    @Bean
    @Primary
    public ReactiveUserDetailsService userDetailsService(UserSecretsEntityRepository secretsRepo) {
        return username -> secretsRepo.findByLogin(username)
                .map(secret -> User.withUsername(secret.getLogin())
                        .password(secret.getPassword())
                        .roles("USER")
                        .build());
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService userDetailsService) {
        return authentication -> {
            return userDetailsService.findByUsername(authentication.getName())
                    .switchIfEmpty(Mono.error(new BadCredentialsException("User not found")))
                    .map(user -> new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,  // Пароль не проверяем
                            user.getAuthorities()
                    ));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}