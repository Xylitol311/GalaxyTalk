package com.galaxytalk.auth.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@RedisHash(value = "jwtToken", timeToLive = 60*60*24*3) //3일 후 자동 삭제
public class RefreshTokenDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Indexed
    private String refreshToken;

    public RefreshTokenDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
