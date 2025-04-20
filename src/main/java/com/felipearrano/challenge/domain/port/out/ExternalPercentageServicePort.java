package com.felipearrano.challenge.domain.port.out;

import reactor.core.publisher.Mono;

public interface ExternalPercentageServicePort {

    Mono<Double> getPercentage();
}
