package com.felipearrano.challenge.infrastructure.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*; // Importa todas las anotaciones de mapeo

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Utilidad para extraer la ruta HTTP de un endpoint interceptado por AOP.
 */
@Component
public class EndpointPathUtil {

    private static final Logger log = LoggerFactory.getLogger(EndpointPathUtil.class);

    public String getEndpointPath(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Obtener el path base de @RequestMapping a nivel de clase (si existe)
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(targetClass, RequestMapping.class);
        String basePath = (classMapping != null && classMapping.value().length > 0) ? classMapping.value()[0] : "";

        // Buscar la anotación de mapeo a nivel de método y obtener su path
        String methodPath = Stream.of(
                        AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class),
                        AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) // Fallback
                )
                .filter(java.util.Objects::nonNull) // Filtrar los nulos
                .findFirst() // Tomar la primera que encuentre
                .map(annotation -> {
                    try {
                        // Intentar obtener el path del atributo 'value' o 'path'
                        String[] values = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
                        if (values.length > 0) return values[0];
                        String[] paths = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
                        if (paths.length > 0) return paths[0];
                        return ""; // Anotación sin path definido
                    } catch (Exception e) {
                        log.warn("No se pudo extraer el path de la anotación de mapeo", e);
                        return "";
                    }
                })
                .orElse(""); // No se encontró anotación de mapeo en el método

        // Combinar paths base y de método
        String fullPath;
        if (basePath.isEmpty()) {
            fullPath = methodPath;
        } else if (methodPath.isEmpty()) {
            fullPath = basePath;
        } else {
            // Asegura una sola '/' entre partes
            fullPath = (basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath) +
                    (methodPath.startsWith("/") ? methodPath : "/" + methodPath);
        }

        // Fallback al nombre del método si no se pudo construir el path
        return fullPath.isEmpty() ? signature.toShortString() : fullPath;
    }
}