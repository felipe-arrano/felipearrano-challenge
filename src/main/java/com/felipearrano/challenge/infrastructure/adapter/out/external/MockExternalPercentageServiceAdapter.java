package com.felipearrano.challenge.infrastructure.adapter.out.external;

import com.felipearrano.challenge.domain.port.out.ExternalPercentageServicePort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Component
public class MockExternalPercentageServiceAdapter implements ExternalPercentageServicePort {

    private static final Logger log = LoggerFactory.getLogger(MockExternalPercentageServiceAdapter.class);
    private static final double MOCK_PERCENTAGE = 10.0;
    private static final Duration MOCK_DELAY = Duration.ofMillis(500);
    private static final String PERCENTAGE_CACHE_NAME = "percentageCache";
    private static final String RESILIENCE4J_INSTANCE_NAME = "percentageService";
    private static final double FAILURE_SIMULATION_RATE = 0.5;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Cache cache;

    public MockExternalPercentageServiceAdapter(CircuitBreakerRegistry circuitBreakerRegistry,
                                                RetryRegistry retryRegistry,
                                                CacheManager cacheManager){
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE_NAME);
        this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE_NAME);
        this.cache = cacheManager.getCache(PERCENTAGE_CACHE_NAME);
    }

    @Override
    public Mono<Double> getPercentage() {
        log.info("Iniciando la obtención del porcentaje.");

        return applyResilience(simulateRemoteCall())
                .doOnSuccess(this::cacheSuccessfulResult)
                .onErrorResume(this::handleFailure);
    }

    private Mono<Double> simulateRemoteCall() {
        return Mono.defer(() -> {
            log.info("iniciando llamada el Mock del Servicio Externo.");

            // --- simulamos fallas
            if (Math.random() < FAILURE_SIMULATION_RATE) {
                log.error("Error al llamar al Mock del Servicio Externo.");
                return Mono.error(new RuntimeException("Error simulado del servicio externo."));
            }
            // --- simulamos fallas

            return Mono.delay(MOCK_DELAY)
                    .then(Mono.fromSupplier(() -> {
                        log.info("El Servicio Respondió: {}%", MOCK_PERCENTAGE);
                        return MOCK_PERCENTAGE;
                    }));
        });
    }

    private Mono<Double> applyResilience(Mono<Double> originalMono) {
        return originalMono
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }

    private void cacheSuccessfulResult(Double value) {
        log.debug("Se obtiene el porcentaje. Se guarda en el cache: {}", value);
        cache.put(PERCENTAGE_CACHE_NAME, value);
    }

    private Mono<Double> handleFailure(Throwable throwable) {
        log.warn("Falla los 3 intentos al servicio externo. Error: {}", throwable.getMessage());
        Cache.ValueWrapper valueWrapper = cache.get(PERCENTAGE_CACHE_NAME); // Fixed key

        if (valueWrapper != null) {
            Double cachedValue = (Double) valueWrapper.get();
            log.warn("se obtiene el porcentaje del cache. Valor en el cache: {}", cachedValue);
            return Mono.just(cachedValue);
        } else {
            log.error("No se puede obtener el valor del cache '{}'.", PERCENTAGE_CACHE_NAME);

            return Mono.error(new RuntimeException("El porcentaje no esta disponible en el servicio externo, ni tampoco en el cache..", throwable));
        }
    }
}
