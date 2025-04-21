package com.felipearrano.challenge.infrastructure.config; // O el paquete que prefieras

import com.fasterxml.jackson.databind.ObjectMapper; // Importa ObjectMapper
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Double> reactiveRedisTemplateForDouble(
            ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) { // Inyecta ObjectMapper

        // Serializador para las claves (siempre String en este caso)
        StringRedisSerializer keySerializer = StringRedisSerializer.UTF_8;

        // Serializador para los valores (Double). Usaremos JSON.
        // Necesita ObjectMapper, que Spring Boot Webflux ya configura.
        Jackson2JsonRedisSerializer<Double> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Double.class);

        // Construir el contexto de serialización
        RedisSerializationContext.RedisSerializationContextBuilder<String, Double> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Double> context = builder
                .value(valueSerializer) // Usa el serializador JSON para el valor
                .hashValue(valueSerializer) // Configura también para hashes si los usaras
                .build();

        // Crear y devolver el template con la fábrica de conexión y el contexto de serialización
        return new ReactiveRedisTemplate<>(factory, context);
    }
}