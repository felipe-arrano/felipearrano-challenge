package com.felipearrano.challenge.infrastructure.aspect;

import com.felipearrano.challenge.domain.HistoryLog;
import com.felipearrano.challenge.infrastructure.service.AsyncHistoryLoggerService;
import com.felipearrano.challenge.infrastructure.util.EndpointPathUtil;
import com.felipearrano.challenge.infrastructure.util.JsonSerializationUtil;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
public class ApiLoggingAspect {

    private final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);
    private final AsyncHistoryLoggerService loggerService;
    private final JsonSerializationUtil jsonSerializationUtil;
    private final EndpointPathUtil endpointPathUtil;

    public ApiLoggingAspect(AsyncHistoryLoggerService loggerService,
                            JsonSerializationUtil jsonSerializationUtil,
                            EndpointPathUtil endpointPathUtil) {
        this.loggerService = loggerService;
        this.jsonSerializationUtil = jsonSerializationUtil;
        this.endpointPathUtil = endpointPathUtil;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) && within(com.felipearrano.challenge.infrastructure.adapter.in.web..*)")
    public void restControllerMethods() {}

    @Around("restControllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant startTime = Instant.now();

        String endpoint = endpointPathUtil.getEndpointPath(joinPoint);
        String params = jsonSerializationUtil.safelySerializeArray(joinPoint.getArgs());
        Object result = joinPoint.proceed();

        if (result instanceof Mono) {
            @SuppressWarnings("unchecked")
            Mono<Object> monoResult = (Mono<Object>) result;
            return monoResult.doOnSuccess(response -> {
                handleLogging(startTime, endpoint, params, response, null);
            }).doOnError(error -> {
                handleLogging(startTime, endpoint, params, null, error);
            });
        } else {
            handleLogging(startTime, endpoint, params, result, null);
            return result;
        }
    }

    private record LogDetails(int status, boolean success, String responseBody, String errorMessage) {}

    private void handleLogging(Instant startTime, String endpoint, String params, Object result, Throwable error) {
        try {
            LogDetails details = extractLogDetails(result, error);

            HistoryLog historyLog = new HistoryLog(
                    UUID.randomUUID(),
                    startTime,
                    endpoint,
                    params,
                    details.responseBody(),
                    details.status(),
                    details.success(),
                    details.errorMessage()
            );
            loggerService.logApiCall(historyLog);

        } catch (Exception e) {
            log.error("Error CRÍTICO dentro del aspecto de logging: {}", e.getMessage(), e);
        }
    }

    private LogDetails extractLogDetails(Object result, Throwable error) {
        Integer status = null;
        String responseBody = null;
        boolean success = false;
        String errorMessage = null;

        if (error != null) {
            log.warn("Extrayendo detalles del error: {}", error.getMessage());
            errorMessage = error.getMessage();
            success = false;

            if (error instanceof ResponseStatusException rse) {
                status = rse.getStatusCode().value();
            } else if (error instanceof RequestNotPermitted) {
                status = HttpStatus.TOO_MANY_REQUESTS.value();
            }
            else {
                status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            }
            responseBody = jsonSerializationUtil.safelySerialize("Error: " + errorMessage);

        } else if (result instanceof ResponseEntity<?> responseEntity) {
            log.debug("Extrayendo detalles desde ResponseEntity");
            status = responseEntity.getStatusCode().value();
            success = responseEntity.getStatusCode().is2xxSuccessful();
            responseBody = responseEntity.hasBody()
                    ? jsonSerializationUtil.safelySerialize(responseEntity.getBody())
                    : "[No Body]";
        } else {
            log.debug("Extrayendo detalles desde objeto de resultado simple");
            status = HttpStatus.OK.value();
            success = true;
            responseBody = jsonSerializationUtil.safelySerialize(result);
        }
        status = (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return new LogDetails(status, success, responseBody, errorMessage);
    }
}


