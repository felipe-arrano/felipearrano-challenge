package com.felipearrano.challenge.infraestructure.adapter.external;

import com.felipearrano.challenge.application.port.out.ExternalPercentageServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class MockExternalPercentageServiceAdapter implements ExternalPercentageServicePort {

    private static final Logger log = LoggerFactory.getLogger(MockExternalPercentageServiceAdapter.class);

    private static final double MOCK_PERCENTAGE = 10.0;
    private static final Duration MOCK_DELAY = Duration.ofMillis(500);

    private static final String PERCENTAGE_CACHE_NAME = "percentageCache";

    @Override
    @Cacheable(cacheNames = PERCENTAGE_CACHE_NAME)
    public Mono<Double> getPercentage() {
        log.info("Llamando al Mock del Servicio Externo de Porcentaje");
        return Mono.delay(MOCK_DELAY)
                .then((Mono.fromSupplier(() -> {
                    log.info("Servicio externo respondiendo con: {}%", MOCK_PERCENTAGE);

                    return MOCK_PERCENTAGE;
                })) );
    }
}
