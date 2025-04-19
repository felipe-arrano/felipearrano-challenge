package com.felipearrano.challenge.infraestructure.adapter.out.persistence.repository;

import com.felipearrano.challenge.infraestructure.adapter.out.persistence.entity.HistotyLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ReactiveHistoryLogRepository extends ReactiveCrudRepository<HistotyLogEntity, UUID> {

    Flux<HistotyLogEntity> findByOrderByIdAsc(Pageable pageable);
}
