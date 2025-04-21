package com.felipearrano.challenge.infrastructure.adapter.out.external;

import com.felipearrano.challenge.infrastructure.config.MockServiceProperties;
import com.felipearrano.challenge.infrastructure.adapter.out.external.exception.PercentageServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import java.time.Duration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MockPercentageServiceAdapterTest {

    private final String redisKey = "percentage:test";
    private final Duration redisTtl = Duration.ofMinutes(30);
    private final Double mockPercentage = 10.0;
    private final Double cachedPercentage = 15.0;

    @Mock private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock private RetryRegistry retryRegistry;
    @Mock private ReactiveRedisTemplate<String, Double> reactiveRedisTemplate;
    @Mock private MockServiceProperties properties;
    @Mock private ReactiveValueOperations<String, Double> reactiveValueOps;
    @Mock private CircuitBreaker circuitBreaker;

    private MockPercentageServiceAdapter mockPercentageServiceAdapter;

    @BeforeEach
    void setUp() {
        Retry defaultRetry = Retry.of("testRetry", RetryConfig.ofDefaults());
        when(retryRegistry.retry(anyString())).thenReturn(defaultRetry);

        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        when(circuitBreaker.tryAcquirePermission()).thenReturn(true);

        when(properties.getRedisKey()).thenReturn(redisKey);
        when(properties.getRedisTtl()).thenReturn(redisTtl);
        when(properties.getPercentageValue()).thenReturn(mockPercentage);
        when(properties.getDelay()).thenReturn(Duration.ZERO);
        when(properties.getFailureRate()).thenReturn(0.0);

        when(reactiveRedisTemplate.opsForValue()).thenReturn(reactiveValueOps);
        when(reactiveValueOps.get(anyString())).thenReturn(Mono.empty());
        when(reactiveValueOps.set(anyString(), anyDouble(), any(Duration.class))).thenReturn(Mono.just(true));

        mockPercentageServiceAdapter = new MockPercentageServiceAdapter(
                circuitBreakerRegistry,
                retryRegistry,
                reactiveRedisTemplate,
                properties
        );
    }

    @Test
    @DisplayName("Debe llamar al servicio, obtener valor, guardarlo en Redis y devolverlo")
    void shouldCallServiceSaveToRedisAndReturnValueOnSuccess() {

        when(properties.getFailureRate()).thenReturn(0.0);
        when(reactiveValueOps.set(eq(redisKey), eq(mockPercentage), eq(redisTtl))).thenReturn(Mono.just(true));

        Mono<Double> resultMono = mockPercentageServiceAdapter.getPercentage();

        StepVerifier.create(resultMono)
                .expectNext(mockPercentage)
                .verifyComplete();

        verify(reactiveValueOps).set(eq(redisKey), eq(mockPercentage), eq(redisTtl));
        verify(reactiveValueOps, never()).get(anyString());
    }

    @Test
    @DisplayName("Debe fallar la llamada al servicio y devolver valor de Redis si existe")
    void shouldUseRedisValueWhenServiceFailsAndCacheExists() {

        when(properties.getFailureRate()).thenReturn(1.0);
        when(reactiveValueOps.get(eq(redisKey))).thenReturn(Mono.just(cachedPercentage));

        Mono<Double> resultMono = mockPercentageServiceAdapter.getPercentage();

        StepVerifier.create(resultMono)
                .expectNext(cachedPercentage)
                .verifyComplete();

        verify(reactiveValueOps).get(eq(redisKey));
        verify(reactiveValueOps, never()).set(anyString(), anyDouble(), any(Duration.class));
    }

    @Test
    @DisplayName("Debe fallar la llamada al servicio y devolver error si Redis está vacío")
    void shouldReturnErrorWhenServiceFailsAndCacheIsEmpty() {

        when(properties.getFailureRate()).thenReturn(1.0);

        Mono<Double> resultMono = mockPercentageServiceAdapter.getPercentage();

        StepVerifier.create(resultMono)
                .expectError(PercentageServiceUnavailableException.class)
                .verify();

        verify(reactiveValueOps).get(eq(redisKey));
        verify(reactiveValueOps, never()).set(anyString(), anyDouble(), any(Duration.class));
    }
}