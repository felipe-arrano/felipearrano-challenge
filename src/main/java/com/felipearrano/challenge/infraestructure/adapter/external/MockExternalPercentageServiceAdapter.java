package com.felipearrano.challenge.infraestructure.adapter.external;

import com.felipearrano.challenge.application.port.out.ExternalPercentageServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class MockExternalPercentageServiceAdapter implements ExternalPercentageServicePort {

    private static final Logger log = LoggerFactory.getLogger(MockExternalPercentageServiceAdapter.class);

    private static final double MOCK_PERCENTAGE = 10.0;
    private static final Duration MOCK_DELAY = Duration.ofMillis(500);

    @Override
    public Mono<Double> getPercentage() {
        log.info("Llamando al Mock del Servicio Externo de Porcentaje");
        return Mono.delay(MOCK_DELAY)
                .then((Mono.fromSupplier(() -> {
                    log.info("Servicio externo respondiendo con: {}%", MOCK_PERCENTAGE);

                    return MOCK_PERCENTAGE;
                })) );
    }
}
