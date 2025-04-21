package com.felipearrano.challenge.infrastructure.adapter.out.external;

import com.felipearrano.challenge.application.port.out.PercentageServicePort;
import com.felipearrano.challenge.infrastructure.adapter.out.external.exception.PercentageServiceUnavailableException;
import com.felipearrano.challenge.infrastructure.config.MockServiceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Component
public class MockPercentageServiceAdapter implements PercentageServicePort {

    private static final Logger log = LoggerFactory.getLogger(MockPercentageServiceAdapter.class);

    private static final String RESILIENCE4J_INSTANCE_NAME = "percentageService";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final ReactiveRedisTemplate<String, Double> reactiveRedisTemplate;
    private final MockServiceProperties properties;

    public MockPercentageServiceAdapter(CircuitBreakerRegistry circuitBreakerRegistry,
                                        RetryRegistry retryRegistry,
                                        ReactiveRedisTemplate<String, Double> reactiveRedisTemplate,
                                        MockServiceProperties properties){
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE_NAME);
        this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE_NAME);
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.properties = properties;
    }

    @Override
    public Mono<Double> getPercentage() {
        log.info("Intentando obtener porcentaje del servicio externo (con fallback a Redis).");
        Mono<Double> serviceCallMono = applyResilience(simulateRemoteCall());

        return serviceCallMono
                .flatMap(valueFromService -> {
                    log.info("Llamada al servicio exitosa. Valor: {}. Actualizando caché Redis Key '{}' con TTL {}.",
                            valueFromService, properties.getRedisKey(), properties.getRedisTtl());
                    return reactiveRedisTemplate.opsForValue()
                            .set(properties.getRedisKey(), valueFromService, properties.getRedisTtl())
                            .thenReturn(valueFromService);
                })
                .onErrorResume(this::fallbackToRedisCache);
    }

    private Mono<Double> simulateRemoteCall() {
        return Mono.defer(() -> {
            log.debug("Intentando llamada simulada al servicio externo...");

            if (Math.random() < properties.getFailureRate()) {
                log.warn("Simulando fallo del servicio externo.");
                return Mono.error(new RuntimeException("Error simulado del servicio externo."));
            }
            return Mono.delay(properties.getDelay())
                    .then(Mono.fromSupplier(() -> {
                        double percentage = properties.getPercentageValue();
                        log.info("Llamada simulada al servicio externo exitosa. Valor: {}%", percentage);
                        return percentage;
                    }));
        });
    }

    private Mono<Double> applyResilience(Mono<Double> originalMono) {
        return originalMono
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    private Mono<Double> fallbackToRedisCache(Throwable throwable) {
        log.warn("La llamada al servicio externo falló después de aplicar resiliencia ({}). Intentando fallback a caché Redis Key '{}'...",
                throwable.getClass().getSimpleName(), properties.getRedisKey());

        return reactiveRedisTemplate.opsForValue().get(properties.getRedisKey())
                .doOnNext(cachedValue -> log.warn("Fallback exitoso: Se recuperó el valor de Redis Key '{}': {}", properties.getRedisKey(), cachedValue)) // Usa properties.getRedisKey()
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Fallback fallido: El servicio externo falló y no hay valor en Redis Key '{}'.",
                            properties.getRedisKey());
                    return Mono.error(new PercentageServiceUnavailableException(
                            "El servicio externo no está disponible y no hay valor en caché Redis.", throwable));
                }));
    }
}