package com.felipearrano.challenge.application.port.out;

import reactor.core.publisher.Mono;

public interface PercentageServicePort {

    Mono<Double> getPercentage();
}
