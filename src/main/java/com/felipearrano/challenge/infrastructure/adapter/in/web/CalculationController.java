package com.felipearrano.challenge.infrastructure.adapter.in.web;

import com.felipearrano.challenge.application.port.in.CalculateSumUseCase;
import com.felipearrano.challenge.infrastructure.adapter.in.web.dto.CalculationResponse;
import com.felipearrano.challenge.infrastructure.adapter.in.web.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "API de Cálculos", description = "Endpoints para realizar cálculos.")
public class CalculationController {

    private static final Logger log = LoggerFactory.getLogger(CalculationController.class);

    private final CalculateSumUseCase calculateSumUseCase;

    public CalculationController(CalculateSumUseCase calculateSumUseCase){
        this.calculateSumUseCase = calculateSumUseCase;
    }

    @Operation(
            summary = "Calcular Suma con Porcentaje Externo",
            description = "Recibe dos números no negativos, los suma y aplica un porcentaje obtenido de un servicio externo (simulado)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cálculo exitoso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CalculationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos (ej. nulos, negativos, no numéricos)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Límite de solicitudes excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Servicio externo no disponible y sin fallback de caché",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/sum-with-percentage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<CalculationResponse>> calculate(
            @Parameter(description = "Primer número para la suma. Debe ser no negativo.", required = true, example = "10.5")
            @RequestParam @NotNull @DecimalMin(value = "0.0", inclusive = true, message = "num1 debe ser positivo o cero") BigDecimal num1,

            @Parameter(description = "Segundo número para la suma. Debe ser no negativo.", required = true, example = "20.0")
            @RequestParam @NotNull @DecimalMin(value = "0.0", inclusive = true, message = "num2 debe ser positivo o cero") BigDecimal num2) {

        log.info("Recibida solicitud GET /sum-with-percentage con num1={}, num2={}", num1, num2);

        return calculateSumUseCase.calculateSumWithPercentage(num1, num2)
                .map(result -> {
                    CalculationResponse responseDto = new CalculationResponse(result);
                    log.info("Cálculo exitoso, devolviendo resultado: {}", result);
                    return ResponseEntity.ok(responseDto); //
                })
                .doOnError(error -> log.error("Error procesando la solicitud: {}", error.getMessage()));
    }
}
