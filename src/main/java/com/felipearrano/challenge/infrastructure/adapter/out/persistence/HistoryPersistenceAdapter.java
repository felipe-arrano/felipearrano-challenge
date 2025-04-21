package com.felipearrano.challenge.infrastructure.adapter.out.persistence;

import java.util.List;
import com.felipearrano.challenge.application.port.out.HistoryRepositoryPort;
import com.felipearrano.challenge.domain.HistoryLog;
import com.felipearrano.challenge.infrastructure.adapter.out.persistence.entity.HistoryLogEntity;
import com.felipearrano.challenge.infrastructure.adapter.out.persistence.mapper.HistoryLogMapper;
import com.felipearrano.challenge.infrastructure.adapter.out.persistence.repository.ReactiveHistoryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.stream.Collectors;

@Component
public class HistoryPersistenceAdapter implements HistoryRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(HistoryPersistenceAdapter.class);

    private final ReactiveHistoryLogRepository historyLogRepository;
    private final HistoryLogMapper historyLogMapper;

    public HistoryPersistenceAdapter(ReactiveHistoryLogRepository historyLogRepository, HistoryLogMapper historyLogMapper) {
        this.historyLogRepository = historyLogRepository;
        this.historyLogMapper = historyLogMapper;
    }

    @Override
    public Mono<Void> saveLog(HistoryLog logToSave) {
        log.debug("Guardando log en BD: {}", logToSave.id());

        HistoryLogEntity entity = historyLogMapper.toEntity(logToSave);

        return historyLogRepository.save(entity)
                .doOnError(e -> log.error("Error al guardar log con ID {}: {}", entity.getId(), e.getMessage()))
                .then();
    }

    @Override
    public Mono<Page<HistoryLog>> findAllPaginated(Pageable pageable) {
        log.debug("Buscando historial paginado: {}", pageable);

        Mono<List<HistoryLogEntity>> pageContentMono = historyLogRepository.findByOrderByIdAsc(pageable)
                .collectList();

        Mono<Long> totalCountMono = historyLogRepository.count();

        return Mono.zip(pageContentMono, totalCountMono)
                .map(tuple -> {
                    List<HistoryLogEntity> entityList = tuple.getT1();
                    long totalCount = tuple.getT2();

                    List<HistoryLog> domainList = entityList.stream()
                            .map(historyLogMapper::toDomain)
                            .collect(Collectors.toList());

                    log.debug("Encontrados {} logs en la página, total {}", domainList.size(), totalCount);

                    Page<HistoryLog> pageResult = new PageImpl<>(domainList, pageable, totalCount);

                    return pageResult;
                })
                .doOnError(e -> log.error("Error al buscar historial paginado: {}", e.getMessage()));
    }
}