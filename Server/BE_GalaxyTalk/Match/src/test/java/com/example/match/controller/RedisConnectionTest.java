package com.example.match.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RedisConnectionTest {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRedisConnection() {
        // Redis에 데이터 저장
        redisTemplate.opsForValue().set("testKey", "Hello Redis!");

        // Redis에서 데이터 가져오기
        String value = (String) redisTemplate.opsForValue().get("testKey");

        // 값 확인
        assertThat(value).isEqualTo("Hello Redis!");
    }
}
