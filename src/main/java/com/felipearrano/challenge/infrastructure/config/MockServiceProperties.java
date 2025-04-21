package com.felipearrano.challenge.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "mock.percentage-service")
@Validated
@Data
public class MockServiceProperties {

    @NotNull(message = "El valor del porcentaje mock no puede ser nulo")
    private Double percentageValue;

    @NotNull(message = "El delay simulado no puede ser nulo")
    private Duration delay = Duration.ofMillis(500);

    @NotBlank(message = "La clave de Redis no puede estar vacía")
    private String redisKey = "percentage:current";

    @NotNull(message = "El TTL de Redis no puede ser nulo")
    private Duration redisTtl = Duration.ofMinutes(30);

    @Min(value = 0, message = "La tasa de fallo debe ser mínimo 0.0")
    @Max(value = 1, message = "La tasa de fallo debe ser máximo 1.0")
    private double failureRate = 0.5;
}