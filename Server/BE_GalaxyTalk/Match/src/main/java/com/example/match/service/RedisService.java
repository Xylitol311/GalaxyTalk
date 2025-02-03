package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_KEY_PREFIX = "user:";
    private static final String MATCH_KEY_PREFIX = "match:";

    public void saveUserStatus(UserMatchStatus user) {
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);
    }

    public UserMatchStatus getUserStatus(String userId) {
        return (UserMatchStatus) redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
    }

    public void deleteUserStatus(String userId) {
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    public void saveMatchInfo(String matchId, List<String> userIds) {
        redisTemplate.opsForValue().set(MATCH_KEY_PREFIX + matchId, userIds);
    }

    public List<String> getMatchInfo(String matchId) {
        return (List<String>) redisTemplate.opsForValue().get(MATCH_KEY_PREFIX + matchId);
    }

    public void deleteMatchInfo(String matchId) {
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }
}
