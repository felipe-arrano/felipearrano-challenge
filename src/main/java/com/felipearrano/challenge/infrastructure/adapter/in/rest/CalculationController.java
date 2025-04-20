package com.felipearrano.challenge.infrastructure.adapter.in.rest;

import com.felipearrano.challenge.domain.port.in.CalculateSumUseCasePort;
import com.felipearrano.challenge.infrastructure.adapter.in.rest.dto.CalculationResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/calculations")
@Validated
public class CalculationController {

    private static final Logger log = LoggerFactory.getLogger(CalculationController.class);

    private final CalculateSumUseCasePort calculateSumUseCasePort;

    public CalculationController(CalculateSumUseCasePort calculateSumUseCasePort){
        this.calculateSumUseCasePort = calculateSumUseCasePort;
    }

    @GetMapping(value = "/sum-with-percentage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CalculationResponse>> calculate(
            @RequestParam @NotNull @DecimalMin(value = "0.0", inclusive = true, message = "num1 debe ser positivo o cero") BigDecimal num1,
            @RequestParam @NotNull @DecimalMin(value = "0.0", inclusive = true, message = "num2 debe ser positivo o cero") BigDecimal num2) {

        log.info("Recibida solicitud GET /sum-with-percentage con num1={}, num2={}", num1, num2);

        return calculateSumUseCasePort.calculateSumWithPercentage(num1, num2)
                .map(result -> {
                    CalculationResponse responseDto = new CalculationResponse(result);
                    log.info("Cálculo exitoso, devolviendo resultado: {}", result);
                    return ResponseEntity.ok(responseDto); //
                })
                .doOnError(error -> log.error("Error procesando la solicitud: {}", error.getMessage()));
    }
}
