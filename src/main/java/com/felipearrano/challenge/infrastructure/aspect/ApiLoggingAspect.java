package com.felipearrano.challenge.infrastructure.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipearrano.challenge.domain.model.HistoryLog;
import com.felipearrano.challenge.infrastructure.log.AsyncHistoryLoggerService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Aspect
@Component
public class ApiLoggingAspect {

    private final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);
    private final AsyncHistoryLoggerService loggerService;
    private final ObjectMapper objectMapper;

    public ApiLoggingAspect(AsyncHistoryLoggerService loggerService, ObjectMapper objectMapper){
        this.loggerService = loggerService;
        this.objectMapper = objectMapper;
    }

    // Define un pointcut que captura la ejecución de cualquier método público
    // dentro de clases anotadas con @RestController en el paquete web de infraestructura
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) && within(com.felipearrano.challenge.infrastructure.adapter.in.rest..*)")
    public void restControllerMethods() {}

    // Define el advice @Around que se ejecuta alrededor de los métodos capturados por el pointcut
    @Around("restControllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant startTime = Instant.now();
        String endpoint = getEndpointPath(joinPoint);
        String params = serializeArguments(joinPoint.getArgs()); // Serializa los argumentos

        // Ejecuta el método original del controlador
        Object result = joinPoint.proceed();

        // Si el resultado es un Mono (como en nuestros endpoints reactivos)
        if (result instanceof Mono) {
            @SuppressWarnings("unchecked") // Necesario por el casteo genérico de Mono
            Mono<Object> monoResult = (Mono<Object>) result;

            // Usamos .doOnTerminate para ejecutar código cuando el Mono termine (éxito o error)
            // o .doFinally que es similar. .doOnTerminate es más seguro para obtener el resultado/error.
            return monoResult.doOnSuccess(response -> {
                // Éxito: Loguear respuesta exitosa
                handleLogging(startTime, endpoint, params, response, null);
            }).doOnError(error -> {
                // Error: Loguear respuesta de error
                handleLogging(startTime, endpoint, params, null, error);
            });
        } else {
            // Si el método no fuera reactivo (no es nuestro caso ahora)
            handleLogging(startTime, endpoint, params, result, null);
            return result;
        }
    }

    // Método helper para encapsular la lógica de creación y envío del log
    private void handleLogging(Instant startTime, String endpoint, String params, Object result, Throwable error) {
        try {
            UUID logId = UUID.randomUUID();
            Integer status = null;
            String responseBody = null;
            boolean success = false;
            String errorMessage = null;

            if (error != null) {
                // Hubo una excepción durante la ejecución del endpoint
                log.warn("Endpoint {} falló con error: {}", endpoint, error.getMessage());
                errorMessage = error.getMessage();
                // Podríamos intentar obtener un status de una excepción HTTP específica si la hubiera
                status = 500; // Asumir Internal Server Error por defecto
                success = false;
                responseBody = serializeObject("Error: " + errorMessage); // Serializa el mensaje de error
            } else if (result instanceof ResponseEntity) {
                // El endpoint devolvió un ResponseEntity (nuestro caso)
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                status = responseEntity.getStatusCode().value();
                success = responseEntity.getStatusCode().is2xxSuccessful();
                if (responseEntity.hasBody()) {
                    responseBody = serializeObject(responseEntity.getBody());
                }
            } else {
                // El endpoint devolvió otra cosa (poco probable en nuestro caso)
                status = 200; // Asumir OK
                success = true;
                responseBody = serializeObject(result);
            }

            HistoryLog historyLog = new HistoryLog(
                    logId,
                    startTime,
                    endpoint,
                    params,
                    responseBody,
                    status,
                    success,
                    errorMessage
            );

            // Llama al servicio asíncrono para guardar el log
            loggerService.logApiCall(historyLog);

        } catch (Exception e) {
            // ¡Importante! Capturar cualquier error aquí para no romper la respuesta original
            log.error("Error CRÍTICO dentro del aspecto de logging. Esto NO debe pasar. Causa: {}", e.getMessage(), e);
        }
    }

    // Método helper para serializar objetos a JSON String de forma segura
    private String serializeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        // Filtra argumentos que no quieres serializar si es necesario
        String serializedArgs = Arrays.stream(args)
                .map(this::serializeObject)
                .collect(Collectors.joining(", "));
        return "[" + serializedArgs + "]";
    }

    private String serializeObject(Object obj) {
        if (obj == null) return "null";
        try {
            // Evita serializar tipos que no sean DTOs simples o primitivos si causan problemas
            if (obj instanceof Mono || obj instanceof Flux) return obj.getClass().getSimpleName(); // No serializar Monos/Fluxes directamente
            // Añadir otras clases a ignorar si es necesario

            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar el objeto a JSON: {}", e.getMessage());
            return "[Serialization Error]";
        } catch (Exception e) {
            log.warn("Error inesperado serializando objeto: {}", e.getMessage());
            return "[Serialization Error]";
        }
    }

    // --- NUEVO MÉTODO HELPER ---
    private String getEndpointPath(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Obtener el path base de @RequestMapping a nivel de clase (si existe)
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(targetClass, RequestMapping.class);
        String basePath = (classMapping != null && classMapping.value().length > 0) ? classMapping.value()[0] : "";

        // Buscar la anotación de mapeo a nivel de método (@GetMapping, @PostMapping, etc.) y obtener su path
        String methodPath = Stream.of(
                        AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) // Fallback por si usan @RequestMapping en método
                )
                .filter(java.util.Objects::nonNull) // Filtrar los nulos (solo habrá una anotación de mapeo)
                .findFirst() // Tomar la primera que encuentre
                .map(annotation -> {
                    try {
                        // Intentar obtener el path del atributo 'value' o 'path' de la anotación
                        String[] values = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
                        if (values.length > 0) return values[0];
                        String[] paths = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
                        if (paths.length > 0) return paths[0];
                        return ""; // La anotación no tiene path definido (ej. @GetMapping sin path)
                    } catch (Exception e) {
                        log.warn("No se pudo extraer el path de la anotación de mapeo", e);
                        return "";
                    }
                })
                .orElse(""); // No se encontró ninguna anotación de mapeo en el método

        // Combinar el path base y el path del método cuidadosamente
        // Asegura que haya una sola '/' entre ellos si ambos existen
        String fullPath;
        if (basePath.isEmpty()) {
            fullPath = methodPath;
        } else if (methodPath.isEmpty()) {
            fullPath = basePath;
        } else {
            fullPath = (basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath) +
                    (methodPath.startsWith("/") ? methodPath : "/" + methodPath);
        }

        // Si por alguna razón no se pudo construir el path, devuelve el nombre del método como fallback
        return fullPath.isEmpty() ? signature.toShortString() : fullPath;
    }
    // --- FIN NUEVO MÉTODO HELPER ---
}


