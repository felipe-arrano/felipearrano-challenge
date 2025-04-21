package com.felipearrano.challenge.infrastructure.service;

import com.felipearrano.challenge.domain.HistoryLog;
import com.felipearrano.challenge.application.port.out.HistoryRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncHistoryLoggerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncHistoryLoggerService.class);
    private final HistoryRepositoryPort historyRepositoryPort;

    public AsyncHistoryLoggerService(HistoryRepositoryPort historyRepositoryPort){
        this. historyRepositoryPort = historyRepositoryPort;
    }

    @Async
    public void logApiCall(HistoryLog historyLogData){
        log.info("Llamada asíncrona para guardar log: {}", historyLogData.id());

        try {
            historyRepositoryPort.saveLog(historyLogData)
                    .doOnError(e -> log.error("Error guardando log asíncrono con ID {}: {}", historyLogData.id(), e.getMessage()))
                    .block();
            log.debug("Log asíncrono guardado exitosamente: {}", historyLogData.id());
        } catch (Exception e) {
            log.error("Excepción inesperada al guardar log asíncrono con ID {}: {}", historyLogData.id(), e.getMessage(), e);
        }
    }
}
