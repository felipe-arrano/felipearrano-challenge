package com.felipearrano.challenge.infrastructure.adapter.out.persistence.repository;

import com.felipearrano.challenge.infrastructure.adapter.out.persistence.entity.HistoryLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ReactiveHistoryLogRepository extends ReactiveCrudRepository<HistoryLogEntity, UUID> {

    Flux<HistoryLogEntity> findByOrderByIdAsc(Pageable pageable);
}
