package com.felipearrano.challenge.infrastructure.adapter.in.web;

import com.felipearrano.challenge.application.port.in.CalculateSumUseCase;
import com.felipearrano.challenge.infrastructure.adapter.in.web.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CalculationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CalculateSumUseCase calculateSumUseCase;

    @Test
    @DisplayName("GET /sum-with-percentage debe devolver 200 OK")
    void calculate_shouldReturn200_whenUseCaseSucceeds() {
        BigDecimal result = new BigDecimal("11.0");
        when(calculateSumUseCase.calculateSumWithPercentage(any(), any())).thenReturn(Mono.just(result));
        webTestClient.get().uri("/api/v1/calculations/sum-with-percentage?num1=5&num2=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.result").isEqualTo(result);
    }

    @Test
    @DisplayName("GET /sum-with-percentage debe devolver 400 si num1 es negativo")
    void calculate_shouldReturn400_whenNum1IsNegative() {
        webTestClient.get().uri("/api/v1/calculations/sum-with-percentage?num1=-1&num2=5")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> {
                    assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(error.message()).isEqualTo("Error de validación en los parámetros de entrada.");
                    assertThat(error.details()).isNotNull(); // Asegura que la lista no sea nula
                    assertThat(error.details()).contains("num1 debe ser positivo o cero");
                });
    }
}