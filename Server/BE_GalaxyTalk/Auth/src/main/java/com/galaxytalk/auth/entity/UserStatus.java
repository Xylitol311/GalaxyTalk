package com.galaxytalk.auth.entity;

import org.apache.catalina.User;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import lombok.*;

@RedisHash("user_status")  // Redis에서 저장될 Key prefix
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class UserStatus {

    @Id
    private String serialNumber;  // 🔥 Redis에서 Key 역할을 함

    private String userInteractionState;

}
