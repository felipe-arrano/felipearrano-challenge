package com.felipearrano.challenge.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) {

        StringRedisSerializer keySerializer = StringRedisSerializer.UTF_8;

        Jackson2JsonRedisSerializer<Double> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Double.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Double> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Double> context = builder
                .value(valueSerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}