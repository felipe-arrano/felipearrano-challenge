package com.felipearrano.challenge.infrastructure.adapter.in.web;

import com.felipearrano.challenge.application.port.in.GetHistoryUseCase;

import com.felipearrano.challenge.domain.HistoryLog;
import com.felipearrano.challenge.infrastructure.adapter.in.web.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "API de Historial", description = "Endpoint para recuperar el historial de llamadas a la API.")
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    private final GetHistoryUseCase getHistoryUseCase;

    public HistoryController(GetHistoryUseCase getHistoryUseCase){
        this.getHistoryUseCase = getHistoryUseCase;
    }

    @Operation(
            summary = "Obtener Historial de Llamadas API",
            description = "Recupera una lista paginada de las llamadas pasadas a la API registradas en el sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial recuperado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class))), // Schema genérico Page
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Límite de solicitudes excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor recuperando el historial",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Page<HistoryLog>>> getHistory(
            @Parameter(description = "Número de página a recuperar (basado en 0).", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Número de elementos por página.", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) int size
    ){
        log.info("Recibida solicitud GET /history con page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return getHistoryUseCase.getHistory(pageable)
                .map(p -> {
                    log.info("Historial encontrado. Devolviendo página {} de {} con {} elementos (total {}).",
                            p.getNumber(), p.getTotalPages(), p.getNumberOfElements(), p.getTotalElements());
                    return ResponseEntity.ok(p);
                })
                .doOnError(error -> log.error("Error al obtener historial paginado: {}", error.getMessage()));
    }
}
