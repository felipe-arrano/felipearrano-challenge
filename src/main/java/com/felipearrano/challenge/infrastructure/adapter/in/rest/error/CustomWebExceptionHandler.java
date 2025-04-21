package com.felipearrano.challenge.infrastructure.adapter.in.rest.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipearrano.challenge.infrastructure.adapter.in.rest.dto.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2) // MUY IMPORTANTE: Orden alto para ejecutarse ANTES que el handler por defecto de Spring Boot, Revisar configurar presedencias en archivo de configuracion
public class CustomWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomWebExceptionHandler.class);
    private final ObjectMapper objectMapper; // Para serializar el ErrorResponse a JSON

    public CustomWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();

        if (ex instanceof RequestNotPermitted) {
            log.warn("Rate limit excedido (Manejado por WebExceptionHandler): {}", ex.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Se ha excedido el límite de solicitudes permitidas.",
                    path
            );

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            try {
                byte[] jsonBytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(jsonBytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (JsonProcessingException e) {
                log.error("Error al serializar ErrorResponse para RequestNotPermitted", e);
                return exchange.getResponse().setComplete();
            }
        }
        return Mono.error(ex);
    }
}
