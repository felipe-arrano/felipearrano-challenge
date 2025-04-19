package com.felipearrano.challenge.application.port.out;

import reactor.core.publisher.Mono;

public interface ExternalPercentageServicePort {

    Mono<Double> getPercentage();
}
