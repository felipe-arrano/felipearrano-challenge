package com.felipearrano.challenge.infrastructure.adapter.in.web.error;

import com.felipearrano.challenge.infrastructure.adapter.in.web.dto.ErrorResponse;
import com.felipearrano.challenge.infrastructure.adapter.out.external.exception.PercentageServiceUnavailableException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private  static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ServerWebInputException.class}) // Captura el error de Input/Type mismatch
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        log.warn("Error de tipo de dato en parámetro: {}", ex.getMessage());

        // Intenta obtener un mensaje más específico si está disponible
        String message = "Se proporcionó un valor inválido para un parámetro. Verifique el tipo de dato esperado.";
        if (ex.getReason() != null) {
            message = ex.getReason();
        } else if (ex.getCause() instanceof TypeMismatchException) {
            message = "Error de tipo: " + ex.getCause().getMessage();
        } else if (ex.getCause() instanceof NumberFormatException) {
            message = "Formato de número inválido: " + ex.getCause().getMessage();
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    // Manejador para errores de validación de parámetros (@RequestParam, @PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex, ServerWebExchange exchange){
        log.warn("Error de validación de parámetros: {}", ex.getMessage());
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Error de validación en los parámetros de entrada.",
                exchange.getRequest().getURI().getPath(),
                details
        );
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    // Manejador para errores de validación de cuerpo de solicitud (@RequestBody)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(WebExchangeBindException ex, ServerWebExchange exchange) {
        log.warn("Error de binding/validación en el cuerpo de la solicitud: {}", ex.getMessage());
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        if (details.isEmpty()) { // A veces el detalle está en errores globales
            details = ex.getBindingResult().getGlobalErrors().stream()
                    .map(globalError -> globalError.getObjectName() + ": " + globalError.getDefaultMessage())
                    .collect(Collectors.toList());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Error de validación o binding en el cuerpo de la solicitud.",
                exchange.getRequest().getURI().getPath(),
                details.isEmpty() ? List.of(ex.getReason()) : details
        );
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    // Manejador para nuestro error específico de servicio externo + caché vacía
    @ExceptionHandler(PercentageServiceUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServiceUnavailable(PercentageServiceUnavailableException ex, ServerWebExchange exchange) {
        log.error("Error 503: Capturada PercentageServiceUnavailableException. Mensaje: {}", ex.getMessage(), ex); // Loguea el mensaje y la traza completa

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "El servicio externo no está disponible temporalmente y no se pudo recuperar de la caché. Intente más tarde.",
                exchange.getRequest().getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        log.error("Error inesperado en la aplicación: {}", ex.getMessage(), ex); // Loguea el stack trace completo
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error interno inesperado en el servidor.", // Mensaje genérico para el cliente
                exchange.getRequest().getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}
