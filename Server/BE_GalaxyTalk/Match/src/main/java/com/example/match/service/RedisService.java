package com.example.match.service;

import com.example.match.domain.MatchResultStatus;
import com.example.match.domain.UserMatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_KEY_PREFIX = "user:";
    private static final String MATCH_KEY_PREFIX = "match:";

    /**
     * 유저 상태를 Redis에 저장
     */
    public void saveUserStatus(UserMatchStatus user) {
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);
    }

    /**
     * Redis에서 유저 상태 조회
     */
    public UserMatchStatus getUserStatus(String userId) {
        return (UserMatchStatus) redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
    }

    /**
     * Redis에서 유저 상태 삭제
     * - Lazy Deletion 시 매칭 취소 혹은 매칭 성공 시점에 호출
     */
    public void deleteUserStatus(String userId) {
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    /**
     * 매칭 정보 저장
     */
    public void saveMatchInfo(String matchId, MatchResultStatus matchResult) {
        redisTemplate.opsForValue().set(MATCH_KEY_PREFIX + matchId, matchResult);
    }

    /**
     * 매칭 정보 조회
     */
    public MatchResultStatus getMatchInfo(String matchId) {
        return (MatchResultStatus) redisTemplate.opsForValue().get(MATCH_KEY_PREFIX + matchId);
    }

    /**
     * 매칭 정보 삭제
     */
    public void deleteMatchInfo(String matchId) {
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }

    /**
     * 매칭 대기 유저를 Sorted Set으로 관리.
     * - 실시간 접속 유저 파악 시 사용
     */
    public void addUserToWaitingQueue(UserMatchStatus userMatchStatus) {
        redisTemplate.opsForZSet().add("waiting_users", userMatchStatus.getUserId(), System.currentTimeMillis());
    }

    /**
     * 매칭 대기 유저 삭제
     * - 매칭 취소 혹은 완료된 경우 실행
     */
    public void removeUserFromWaitingQueue(String userId) {
        redisTemplate.opsForZSet().remove("waiting_users", userId);
    }

    /**
     * 실시간 매칭 대기 유저 중 랜덤으로 조회
     */
    public List<String> getRandomWaitingUsers(int count) {
        List<Object> randomObjects = redisTemplate.opsForZSet().randomMembers("waiting_users", count);

        // Object → String 변환
        return randomObjects.stream()
                .map(obj -> obj.toString())
                .collect(Collectors.toList());
    }

}
