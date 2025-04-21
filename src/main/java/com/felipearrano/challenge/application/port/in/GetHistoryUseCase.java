package com.felipearrano.challenge.application.port.in;

import com.felipearrano.challenge.domain.HistoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface GetHistoryUseCase {
    Mono<Page<HistoryLog>> getHistory(Pageable pageable);
}
