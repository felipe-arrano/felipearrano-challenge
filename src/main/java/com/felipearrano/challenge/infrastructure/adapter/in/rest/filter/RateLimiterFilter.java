package com.felipearrano.challenge.infrastructure.adapter.in.rest.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher; // Importa AntPathMatcher
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimiterFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);
    private static final String RESILIENCE4J_INSTANCE_NAME = "apiGlobalLimiter";

    private final RateLimiter rateLimiter;

    // Define las rutas a excluir del rate limiting
    private final List<String> excludedPaths = List.of(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs/**",     // Incluye el path configurado en application.yml
            "/webjars/**",
            "/actuator/**"     // Excluir endpoints de Actuator también es buena idea
    );
    // Helper para comparar patrones de ruta estilo Ant
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    public RateLimiterFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter(RESILIENCE4J_INSTANCE_NAME);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Verifica si la ruta actual debe ser excluida
        boolean excluded = excludedPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (excluded) {
            log.trace("Path {} excluido del rate limiting.", path);
            return chain.filter(exchange); // Se salta el rate limiter
        }

        // Aplica el rate limiter solo para rutas NO excluidas
        log.trace("Aplicando Rate Limiter para path: {}", path);

        // Usa tryAcquirePermission (no bloqueante)
        boolean permitted = rateLimiter.acquirePermission();

        if (permitted) {
            // Si hay permiso, continúa la cadena
            return chain.filter(exchange);
        } else {
            // Si NO hay permiso, lanza la excepción para que la maneje el Exception Handler
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            RequestNotPermitted exception = RequestNotPermitted.createRequestNotPermitted(rateLimiter);
            log.warn("Rate limit excedido (Detectado por tryAcquirePermission). IP: {}. Path: {}. RateLimiter: {}", clientIp, path, exception.getMessage());
            // Lanza el error para que lo capture CustomWebExceptionHandler o GlobalExceptionHandler
            return Mono.error(exception);
        }
    }
}