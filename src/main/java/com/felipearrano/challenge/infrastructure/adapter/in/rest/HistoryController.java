package com.felipearrano.challenge.infrastructure.adapter.in.rest;

import com.felipearrano.challenge.domain.model.HistoryLog;
import com.felipearrano.challenge.domain.port.out.HistoryRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    private final HistoryRepositoryPort historyRepositoryPort;

    public HistoryController(HistoryRepositoryPort historyRepositoryPort){
        this.historyRepositoryPort = historyRepositoryPort;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Page<HistoryLog>>> getHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            // El sort es opcional, formato: "propiedad,direccion" ej. "timestamp,desc"
            @RequestParam(name = "sort", required = false) String sort) {

        log.info("Recibida solicitud GET /history con page={}, size={}, sort='{}'", page, size, sort);

        Pageable pageable;
        if (sort != null && !sort.isBlank()) {
            try {
                String[] sortParams = sort.split(",");
                String property = sortParams[0];
                Sort.Direction direction = (sortParams.length > 1)
                        ? Sort.Direction.fromString(sortParams[1])
                        : Sort.DEFAULT_DIRECTION;
                pageable = PageRequest.of(page, size, Sort.by(direction, property));
            } catch (Exception e) {
                log.warn("Parámetro sort inválido '{}', usando paginación sin orden. Error: {}", sort, e.getMessage());
                pageable = PageRequest.of(page, size); // Fallback a sin orden si el sort es inválido
            }
        } else {
            pageable = PageRequest.of(page, size); // Sin orden si no se provee sort
        }
        log.debug("Pageable construido: {}", pageable);

        return historyRepositoryPort.findAllPaginated(pageable)
                .map(p -> { // Renombrado lambda param para claridad
                    log.info("Historial encontrado. Devolviendo página {} de {} con {} elementos (total {}).",
                            p.getNumber(), p.getTotalPages(), p.getNumberOfElements(), p.getTotalElements());
                    return ResponseEntity.ok(p);
                })
                .doOnError(error -> log.error("Error al obtener historial paginado: {}", error.getMessage()));
    }
}
