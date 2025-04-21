package com.felipearrano.challenge.infrastructure.adapter.in.rest.filter;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimiterFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    private static final String RESILIENCE4J_INSTANCE_NAME = "apiGlobalLimiter";

    private final RateLimiter rateLimiter;

    @Autowired
    public RateLimiterFilter(RateLimiterRegistry rateLimiterRegistry){
        this.rateLimiter = rateLimiterRegistry.rateLimiter(RESILIENCE4J_INSTANCE_NAME);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.trace("Aplicando Rate Limiter Filter (Reactor Operator) para request: {}", exchange.getRequest().getURI());

        return chain.filter(exchange)
                .transformDeferred(RateLimiterOperator.of(rateLimiter)) // <-- Aplica el operador aquí
                .doOnError(RequestNotPermitted.class, ex -> {
                    String clientIp = exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown";
                    log.warn("Rate limit excedido (Detectado por Operador). IP: {}. RateLimiter: {}", clientIp, ex.getMessage());
                });
    }
}
