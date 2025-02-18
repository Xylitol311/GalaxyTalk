package com.example.match.service;

import com.example.match.domain.MatchResultStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private static final String USER_KEY_PREFIX = "user:";
    private static final String MATCH_KEY_PREFIX = "match:";
    private static final String REJECTION_KEY_PREFIX = "rejected:";
    // Sorted Set 키: 대기 유저 관리 (score는 매칭 시작 시간)
    private static final String WAITING_USERS_KEY = "waiting_users";
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * WAITING 상태인 유저를 원자적으로 IN_PROGRESS 상태로 전환하는 메서드
     * Lua 스크립트를 사용하여 원자적 연산을 보장합니다.
     */
    public boolean atomicTransitionToInProgress(String userId) {
        String key = USER_KEY_PREFIX + userId;
        String script =
                "if redis.call('EXISTS', KEYS[1]) == 1 then " +
                        "  local user = cjson.decode(redis.call('GET', KEYS[1])); " +
                        "  if user.status == 'WAITING' then " +
                        "    user.status = 'IN_PROGRESS'; " +
                        "    redis.call('SET', KEYS[1], cjson.encode(user)); " +
                        "    return 1; " +
                        "  else " +
                        "    return 0; " +
                        "  end " +
                        "else " +
                        "  return 0; " +
                        "end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(key));
        return result != null && result == 1;
    }

    /**
     * 유저 상태를 Redis에 저장합니다.
     */
    public void saveUserStatus(UserMatchStatus user) {
        log.info("유저 상태 저장: {}", user.getUserId());
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);
    }

    /**
     * Redis에서 유저 상태를 조회합니다.
     */
    public UserMatchStatus getUserStatus(String userId) {
        log.info("유저 상태 조회: {}", userId);
        Object data = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        if (data == null) {
            return null;
        }
        if (!(data instanceof UserMatchStatus userMatchStatus)) {
            throw new BusinessException(ErrorCode.REDIS_DATA_MISMATCH,
                    "Redis 데이터 타입 불일치: userId=" + userId);
        }
        return userMatchStatus;
    }

    /**
     * Redis에서 유저 상태를 삭제합니다.
     */
    public void deleteUserStatus(String userId) {
        log.info("유저 상태 삭제: {}", userId);
        redisTemplate.delete(USER_KEY_PREFIX + userId);
        deleteRejection(userId);
    }

    /**
     * 대기 큐(Waiting Pool)에 유저를 추가합니다.
     * score는 현재 시간(매칭 시작 시간)으로 설정합니다.
     */
    public void addUserToWaitingQueue(UserMatchStatus user) {
        log.info("대기 큐에 유저 추가: {}", user.getUserId());
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForZSet().add(WAITING_USERS_KEY, user.getUserId(), user.getStartTime());
    }

    /**
     * 대기 큐에서 유저를 제거합니다.
     */
    public void removeUserFromWaitingQueue(String userId) {
        log.info("대기 큐에서 유저 제거: {}", userId);
        redisTemplate.opsForZSet().remove(WAITING_USERS_KEY, userId);
    }

    /**
     * Redis 대기 큐에서 모든 유저 ID를 조회합니다.
     */
    public Set<String> getAllWaitingUsers() {
        Set<Object> result = redisTemplate.opsForZSet().range(WAITING_USERS_KEY, 0, -1);
        if (result == null) {
            return Collections.emptySet();
        }
        return result.stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * Redis 대기 큐에서 랜덤으로 count만큼의 유저 ID를 조회합니다.
     */
    public Set<String> getRandomWaitingUsers(int count) {
        List<Object> result = redisTemplate.opsForZSet().randomMembers(WAITING_USERS_KEY, count);
        if (result == null) {
            return Collections.emptySet();
        }
        return result.stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * 매칭 정보를 Redis에 저장합니다.
     */
    public void saveMatchInfo(String matchId, MatchResultStatus matchResult) {
        log.info("매칭 정보 저장: {}", matchId);
        if (matchId == null || matchResult == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "matchId 혹은 matchResult가 null입니다.");
        }
        redisTemplate.opsForValue().set(MATCH_KEY_PREFIX + matchId, matchResult);
    }

    /**
     * Redis에서 매칭 정보를 조회합니다.
     */
    public MatchResultStatus getMatchInfo(String matchId) {
        log.info("매칭 정보 조회: {}", matchId);
        Object data = redisTemplate.opsForValue().get(MATCH_KEY_PREFIX + matchId);
        if (data == null) {
            return null;
        }
        if (!(data instanceof MatchResultStatus matchResult)) {
            throw new BusinessException(ErrorCode.REDIS_DATA_MISMATCH,
                    "Redis 데이터 타입 불일치: matchId=" + matchId);
        }
        return matchResult;
    }

    /**
     * Redis에서 매칭 정보를 삭제합니다.
     */
    public void deleteMatchInfo(String matchId) {
        log.info("매칭 정보 삭제: {}", matchId);
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }

    /**
     * userId가 rejectedUserId를 거절한 기록 저장
     */
    public void addRejection(String userId, String rejectedUserId) {
        String key = REJECTION_KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(key, rejectedUserId);
    }

    /**
     * userId가 otherUserId를 과거에 거절한 적이 있는지 조회
     */
    public boolean hasRejected(String userId, String otherUserId) {
        String key = REJECTION_KEY_PREFIX + userId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, otherUserId);
        return isMember != null && isMember;
    }

    /**
     * userId가 rejectedUserId를 거절한 기록 삭제
     */
    public void deleteRejection(String userId) {
        String key = REJECTION_KEY_PREFIX + userId;
        redisTemplate.opsForSet().remove(key);
    }
}
