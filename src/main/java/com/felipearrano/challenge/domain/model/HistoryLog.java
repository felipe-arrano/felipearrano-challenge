package com.felipearrano.challenge.domain.model;

import java.time.Instant;
import java.util.UUID;

public record HistoryLog(
        UUID id,
        Instant timestamp,
        String endpointInvoked,
        String parametersReceived,
        String responseBody,
        Integer httpStatus,
        Boolean isSuccess,
        String errorMessage
) {
}
