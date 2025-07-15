package ru.eventorg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.BadCredentialsException;
import ru.eventorg.exception.ErrorState;
import ru.eventorg.repository.UserSecretsEntityRepository;
import ru.eventorg.security.JwtTokenUtil;

import java.util.List;


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
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/auth/login", "/auth/register").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    private ServerAuthenticationConverter jwtAuthenticationConverter(JwtTokenUtil jwtTokenUtil) {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtTokenUtil.validateToken(token)
                        .flatMap(valid -> {
                            if (!valid) {
                                return Mono.error(new BadCredentialsException(ErrorState.INVALID_TOKEN));
                            }
                            try {
                                String username = jwtTokenUtil.extractUsername(token);
                                return Mono.just(new UsernamePasswordAuthenticationToken(username, null));
                            } catch (Exception e) {
                                return Mono.error(new BadCredentialsException(ErrorState.INVALID_TOKEN));
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
    public ReactiveAuthenticationManager authenticationManager(ReactiveUserDetailsService userDetailsService) {
        return authentication -> userDetailsService.findByUsername(authentication.getName())
                .switchIfEmpty(Mono.error(new BadCredentialsException(ErrorState.BAD_CREDENTIALS)))
                .map(user -> new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        user.getAuthorities()
                ));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://event-organization.ru", "http://1045269-cs76553.tmweb.ru"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}