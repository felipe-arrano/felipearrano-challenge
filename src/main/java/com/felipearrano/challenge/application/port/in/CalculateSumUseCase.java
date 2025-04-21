package com.felipearrano.challenge.application.port.in;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CalculateSumUseCase {

    Mono<BigDecimal> calculateSumWithPercentage(BigDecimal num1, BigDecimal num2);
}
