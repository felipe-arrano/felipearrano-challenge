package com.felipearrano.challenge.application.usecase;

import com.felipearrano.challenge.application.port.in.CalculateSumUseCasePort;
import com.felipearrano.challenge.application.port.out.ExternalPercentageServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CalculateSumUseCase implements CalculateSumUseCasePort {

    private static final Logger log = LoggerFactory.getLogger(CalculateSumUseCase.class);

    private final ExternalPercentageServicePort externalPercentageServicePort;

    public CalculateSumUseCase(ExternalPercentageServicePort externalPercentageServicePort) {
        this.externalPercentageServicePort = externalPercentageServicePort;
    }

    @Override
    public Mono<BigDecimal> calculateSumWithPercentage(BigDecimal num1, BigDecimal num2) {
        log.info("Iniciando cálculo para num1: {}, num2: {}", num1, num2);

        if (num1 == null || num2 == null) {
            log.error("Input nulo recibido: num1={}, num2={}", num1, num2);
            return Mono.error(new IllegalArgumentException("Los números de entrada no pueden ser nulos."));
        }

        return externalPercentageServicePort.getPercentage()
                .flatMap(percentage -> {
                    log.info("Porcentaje obtenido: {}%", percentage);
                    if (percentage == null) {
                        log.error("El servicio externo devolvió un porcentaje nulo.");
                        return Mono.error(new IllegalStateException("Porcentaje obtenido es nulo."));
                    }

                    BigDecimal sum = num1.add(num2);

                    BigDecimal percentageDecimal = BigDecimal.valueOf(percentage)
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal multiplier = BigDecimal.ONE.add(percentageDecimal);
                    BigDecimal result = sum.multiply(multiplier);

                    log.info("Cálculo: ({} + {}) * (1 + {} / 100) = {}", num1, num2, percentage, result);

                    return Mono.just(result);
                })
                .doOnError(error -> log.error("Error durante el cálculo: {}", error.getMessage()))
                .doOnSuccess(result -> log.info("Cálculo completado exitosamente con resultado: {}", result));
    }
}
