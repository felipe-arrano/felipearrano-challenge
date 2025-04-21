package com.felipearrano.challenge.infrastructure.adapter.in.web.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RateLimiter rateLimiter;
    // Define las rutas a excluir del rate limiting
    private final List<String> excludedPaths = List.of(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/webjars/**",
            "/actuator/**"
    );

    @Autowired
    public RateLimiterFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter(RESILIENCE4J_INSTANCE_NAME);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean excluded = excludedPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (excluded) {
            log.trace("Path {} excluido del rate limiting.", path);
            return chain.filter(exchange);
        }

        log.trace("Aplicando Rate Limiter para path: {}", path);

        boolean permitted = rateLimiter.acquirePermission();

        if (permitted) {
            return chain.filter(exchange);
        } else {
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            RequestNotPermitted exception = RequestNotPermitted.createRequestNotPermitted(rateLimiter);
            log.warn("Rate limit excedido (Detectado por tryAcquirePermission). IP: {}. Path: {}. RateLimiter: {}", clientIp, path, exception.getMessage());
            return Mono.error(exception);
        }
    }
}