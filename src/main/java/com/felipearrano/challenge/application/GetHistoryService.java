package com.felipearrano.challenge.application;

import com.felipearrano.challenge.application.port.in.GetHistoryUseCase;
import com.felipearrano.challenge.application.port.out.HistoryRepositoryPort; // Importa el puerto de SALIDA
import com.felipearrano.challenge.domain.HistoryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetHistoryService implements GetHistoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetHistoryService.class);
    private final HistoryRepositoryPort historyRepositoryPort;

    public GetHistoryService(HistoryRepositoryPort historyRepositoryPort) {
        this.historyRepositoryPort = historyRepositoryPort;
    }

    @Override
    public Mono<Page<HistoryLog>> getHistory(Pageable pageable) {
        log.info("Caso de uso GetHistory ejecutándose para pageable: {}", pageable);

        return historyRepositoryPort.findAllPaginated(pageable);
    }
}
