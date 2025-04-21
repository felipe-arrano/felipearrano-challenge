package com.felipearrano.challenge.application;

import com.felipearrano.challenge.application.port.out.HistoryRepositoryPort;
import com.felipearrano.challenge.domain.HistoryLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetHistoryServiceTest {

    @Mock
    private HistoryRepositoryPort historyRepositoryPort;

    @InjectMocks
    private GetHistoryService getHistoryService;

    private Pageable pageable;
    private HistoryLog log1;
    private HistoryLog log2;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        log1 = new HistoryLog(UUID.randomUUID(), Instant.now(), "/api/test1", "[]", "{}", 200, true, null);
        log2 = new HistoryLog(UUID.randomUUID(), Instant.now(), "/api/test2", "[]", "{}", 200, true, null);
    }

    @Test
    @DisplayName("Debe devolver una página de historial cuando el repositorio responde OK")
    void shouldReturnHistoryPageWhenRepositorySucceeds() {
        // Arrange
        List<HistoryLog> logs = List.of(log1, log2);
        Page<HistoryLog> expectedPage = new PageImpl<>(logs, pageable, 2);
        when(historyRepositoryPort.findAllPaginated(any(Pageable.class)))
                .thenReturn(Mono.just(expectedPage));

        // Act
        Mono<Page<HistoryLog>> resultMono = getHistoryService.getHistory(pageable);

        // Assert
        StepVerifier.create(resultMono)
                .expectNextMatches(page ->
                        page.getTotalElements() == 2 &&
                                page.getContent().size() == 2 &&
                                page.getContent().contains(log1) &&
                                page.getContent().contains(log2)
                )
                .verifyComplete();

        verify(historyRepositoryPort).findAllPaginated(pageable);
    }

    @Test
    @DisplayName("Debe devolver una página vacía cuando el repositorio no encuentra logs")
    void shouldReturnEmptyPageWhenRepositoryFindsNothing() {
        // Arrange
        Page<HistoryLog> emptyPage = Page.empty(pageable);
        when(historyRepositoryPort.findAllPaginated(any(Pageable.class)))
                .thenReturn(Mono.just(emptyPage));

        // Act
        Mono<Page<HistoryLog>> resultMono = getHistoryService.getHistory(pageable);

        // Assert
        StepVerifier.create(resultMono)
                .expectNextMatches(page ->
                        page.getTotalElements() == 0 &&
                                page.getContent().isEmpty()
                )
                .verifyComplete();

        verify(historyRepositoryPort).findAllPaginated(pageable);
    }

    @Test
    @DisplayName("Debe propagar error si el repositorio falla")
    void shouldPropagateErrorWhenRepositoryFails() {
        // Arrange
        RuntimeException simulatedError = new RuntimeException("Error de BD simulado");
        when(historyRepositoryPort.findAllPaginated(any(Pageable.class)))
                .thenReturn(Mono.error(simulatedError));

        // Act
        Mono<Page<HistoryLog>> resultMono = getHistoryService.getHistory(pageable);

        // Assert
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Error de BD simulado"))
                .verify();

        verify(historyRepositoryPort).findAllPaginated(pageable);
    }
}