package com.felipearrano.challenge.application.port.out;

import com.felipearrano.challenge.domain.model.HistoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface HistoryRepositoryPort {

    Mono<Void> saveLog(HistoryLog log);

    Mono<Page<HistoryLog>> findAllPaginated(Pageable pageable);

}
