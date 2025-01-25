package com.example.match.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // Redis 템플릿 객체 생성
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // Redis 연결 설정
        template.setConnectionFactory(connectionFactory);
        // 키를 문자열로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        // 값을 JSON으로 직렬화
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

}
