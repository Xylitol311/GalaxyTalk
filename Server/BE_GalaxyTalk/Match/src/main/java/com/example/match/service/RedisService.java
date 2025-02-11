package com.example.match.service;

import com.example.match.domain.MatchResultStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("save user status");
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);
    }

    /**
     * Redis에서 유저 상태 조회
     */
    public UserMatchStatus getUserStatus(String userId) {
        log.info("Getting user match status...");
        Object data = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        if (data == null) {
            // 존재하지 않는 경우 null을 반환하도록 (비즈니스 로직에서 처리)
            return null;
        }
        // 혹시 데이터가 UserMatchStatus가 아니면 예외 발생
        if (!(data instanceof UserMatchStatus userMatchStatus)) {
            throw new BusinessException(ErrorCode.REDIS_DATA_MISMATCH,
                    "Redis에서 가져온 데이터가 UserMatchStatus 타입이 아닙니다. userId=" + userId);
        }
        return userMatchStatus;
    }


    /**
     * Redis에서 유저 상태 삭제
     * - Lazy Deletion 시 매칭 취소 혹은 매칭 성공 시점에 호출
     */
    public void deleteUserStatus(String userId) {
        log.info("Deleting user match status...");
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    /**
     * 매칭 정보 저장
     */
    public void saveMatchInfo(String matchId, MatchResultStatus matchResult) {
        log.info("Setting match info to " + matchResult);
        if (matchId == null || matchResult == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "matchId 혹은 matchResult가 null입니다.");
        }
        redisTemplate.opsForValue().set(MATCH_KEY_PREFIX + matchId, matchResult);
    }

    /**
     * 매칭 정보 조회
     */
    public MatchResultStatus getMatchInfo(String matchId) {
        log.info("Getting match info...");
        Object data = redisTemplate.opsForValue().get(MATCH_KEY_PREFIX + matchId);
        if (data == null) {
            // 존재하지 않는 경우 null을 반환
            return null;
        }
        // 혹시 데이터 형식이 다르면 예외 발생
        if (!(data instanceof MatchResultStatus matchResult)) {
            throw new BusinessException(ErrorCode.REDIS_DATA_MISMATCH,
                    "Redis에서 가져온 데이터가 MatchResultStatus 타입이 아닙니다. matchId=" + matchId);
        }
        return matchResult;
    }


    /**
     * 매칭 정보 삭제
     */
    public void deleteMatchInfo(String matchId) {
        log.info("Deleting match info...");
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }

    /**
     * 매칭 대기 유저를 Sorted Set으로 관리.
     * - 실시간 접속 유저 파악 시 사용
     */
    public void addUserToWaitingQueue(UserMatchStatus userMatchStatus) {
        log.info("Adding user to waiting queue from Redis...");
        if (userMatchStatus == null || userMatchStatus.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForZSet().add("waiting_users", userMatchStatus.getUserId(), System.currentTimeMillis());
    }

    /**
     * 매칭 대기 유저 삭제
     * - 매칭 취소 혹은 완료된 경우 실행
     */
    public void removeUserFromWaitingQueue(String userId) {
        log.info("Removing user from waiting queue from Redis...");
        redisTemplate.opsForZSet().remove("waiting_users", userId);
    }

    /**
     * 실시간 매칭 대기 유저 중 랜덤으로 조회
     */
    public List<String> getRandomWaitingUsers(int count) {
        log.info("Getting random waiting users...");
        // randomMembers가 null을 반환할 수도 있으므로 안전하게 처리
        List<Object> randomObjects = redisTemplate.opsForZSet().randomMembers("waiting_users", count);
        if (randomObjects == null) {
            return List.of(); // null이면 빈 리스트 반환
        }
        // Object → String 변환
        return randomObjects.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

}
