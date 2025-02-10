package com.galaxytalk.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.sql.Ref;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@RedisHash(value = "jwtToken", timeToLive = 60*60*24*3) //3일 후 자동 삭제
public class RefreshToken {

    @Id
    private String accessToken;

    private String refreshToken;
}
