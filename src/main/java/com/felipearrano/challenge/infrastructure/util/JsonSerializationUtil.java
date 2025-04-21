package com.felipearrano.challenge.infrastructure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class JsonSerializationUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonSerializationUtil.class);
    private final ObjectMapper objectMapper;

    public JsonSerializationUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializa de forma segura un array de objetos a una representación String.
     * Útil para loguear argumentos de métodos.
     * @param args Array de objetos a serializar.
     * @return String representando el array (ej. "[arg1_json, arg2_json]").
     */
    public String safelySerializeArray(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        // Llama al serializador de objeto individual para cada elemento
        String serializedArgs = Arrays.stream(args)
                .map(this::safelySerialize)
                .collect(Collectors.joining(", "));
        return "[" + serializedArgs + "]";
    }

    /**
     * Serializa de forma segura un objeto individual a JSON String.
     * Maneja nulls, evita serializar tipos reactivos directamente, y captura errores.
     * @param obj El objeto a serializar.
     * @return El JSON como String, "null", nombre de clase reactiva, o "[Serialization Error]".
     */
    public String safelySerialize(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof Mono || obj instanceof Flux) {
            return obj.getClass().getSimpleName();
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar el objeto de tipo {} a JSON: {}", obj.getClass().getName(), e.getMessage());
            return "[Serialization Error]";
        } catch (Exception e) {
            // Captura genérica por si acaso
            log.warn("Error inesperado serializando objeto de tipo {}: {}", obj.getClass().getName(), e.getMessage());
            return "[Serialization Error]";
        }
    }
}
