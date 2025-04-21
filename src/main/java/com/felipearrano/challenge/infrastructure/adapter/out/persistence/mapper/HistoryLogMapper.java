package com.felipearrano.challenge.infrastructure.adapter.out.persistence.mapper;

import com.felipearrano.challenge.domain.HistoryLog;
import com.felipearrano.challenge.infrastructure.adapter.out.persistence.entity.HistoryLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HistoryLogMapper {

    HistoryLog toDomain(HistoryLogEntity entity);

    HistoryLogEntity toEntity(HistoryLog domain);
}
