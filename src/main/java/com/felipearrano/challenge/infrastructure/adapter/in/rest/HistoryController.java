package com.felipearrano.challenge.infrastructure.adapter.in.rest;

import com.felipearrano.challenge.domain.model.HistoryLog;
import com.felipearrano.challenge.domain.port.out.HistoryRepositoryPort;
import com.felipearrano.challenge.infrastructure.adapter.in.rest.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "API de Historial", description = "Endpoint para recuperar el historial de llamadas a la API.")
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    private final HistoryRepositoryPort historyRepositoryPort;

    public HistoryController(HistoryRepositoryPort historyRepositoryPort){
        this.historyRepositoryPort = historyRepositoryPort;
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
    public Mono<ResponseEntity<Page<HistoryLog>>> getHistory(@ParameterObject Pageable pageable) {

        log.info("Recibida solicitud GET /history con paginación implícita (Pageable): {}", pageable);

        return historyRepositoryPort.findAllPaginated(pageable)
                .map(page -> {
                    log.info("Historial encontrado. Devolviendo página {} de {} con {} elementos (total {}).",
                            page.getNumber(), page.getTotalPages(), page.getNumberOfElements(), page.getTotalElements());
                    return ResponseEntity.ok(page);
                })
                .doOnError(error -> log.error("Error al obtener historial paginado: {}", error.getMessage()));
    }
}
