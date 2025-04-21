package com.felipearrano.challenge.application;

import com.felipearrano.challenge.application.port.out.PercentageServicePort;
import com.felipearrano.challenge.application.port.in.CalculateSumUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculateSumServiceTest {

    @Mock
    private PercentageServicePort percentageServicePort;
    @InjectMocks
    private CalculateSumService calculateSumService;

    @Test
    @DisplayName("Debe calcular correctamente la suma con porcentaje cuando el servicio externo responde OK")
    void shouldCalculateCorrectlyWhenPercentageServiceSucceeds() {

        BigDecimal num1 = new BigDecimal("5.0");
        BigDecimal num2 = new BigDecimal("5.0");
        Double mockPercentage = 10.0;
        BigDecimal expectedResult = new BigDecimal("11.00");

        when(percentageServicePort.getPercentage()).thenReturn(Mono.just(mockPercentage));

        Mono<BigDecimal> resultMono = calculateSumService.calculateSumWithPercentage(num1, num2);

        StepVerifier.create(resultMono)
                .expectNextMatches(result -> result.compareTo(expectedResult) == 0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Debe devolver error si el servicio de porcentaje falla")
    void shouldReturnErrorWhenPercentageServiceFails() {
        BigDecimal num1 = new BigDecimal("5");
        BigDecimal num2 = new BigDecimal("5");
        RuntimeException simulatedError = new RuntimeException("Fallo simulado del servicio externo");

        when(percentageServicePort.getPercentage()).thenReturn(Mono.error(simulatedError));

        Mono<BigDecimal> resultMono = calculateSumService.calculateSumWithPercentage(num1, num2);

        StepVerifier.create(resultMono)
                .expectError(RuntimeException.class)
                .verify();

        verify(percentageServicePort, times(1)).getPercentage();
    }

    @Test
    @DisplayName("Debe devolver IllegalArgumentException si num1 es nulo")
    void shouldReturnIllegalArgumentExceptionWhenNum1IsNull() {

        BigDecimal num1 = null;
        BigDecimal num2 = new BigDecimal("5");

        Mono<BigDecimal> resultMono = calculateSumService.calculateSumWithPercentage(num1, num2);

        StepVerifier.create(resultMono)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(percentageServicePort, never()).getPercentage();
    }

    @Test
    @DisplayName("Debe devolver IllegalArgumentException si num2 es nulo")
    void shouldReturnIllegalArgumentExceptionWhenNum2IsNull() {

        BigDecimal num1 = new BigDecimal("5");
        BigDecimal num2 = null;

        Mono<BigDecimal> resultMono = calculateSumService.calculateSumWithPercentage(num1, num2);

        StepVerifier.create(resultMono)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(percentageServicePort, never()).getPercentage();
    }
}