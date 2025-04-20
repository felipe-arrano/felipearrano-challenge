package com.felipearrano.challenge.infrastructure.adapter.in.rest;

import com.felipearrano.challenge.domain.model.HistoryLog;
import com.felipearrano.challenge.domain.port.out.HistoryRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
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

  // public Mono<ResponseEntity<Page<HistoryLog>>> getHistory()
}
