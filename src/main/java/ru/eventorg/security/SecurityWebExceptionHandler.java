package ru.eventorg.security;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import ru.eventorg.exception.BadCredentialsException;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)   // должен сработать раньше DefaultErrorWebExceptionHandler (который имеет order -1)
public class SecurityWebExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Аутентификационные ошибки — 401
        if (ex instanceof AuthenticationException) {
            return this.writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
        // Ошибки авторизации — 403
        if (ex instanceof AccessDeniedException) {
            return this.writeErrorResponse(exchange, HttpStatus.FORBIDDEN, ex.getMessage());
        }
        if (ex instanceof BadCredentialsException) {
            return this.writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
        return Mono.error(ex);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String msg) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = (msg).getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes)));
    }
}
