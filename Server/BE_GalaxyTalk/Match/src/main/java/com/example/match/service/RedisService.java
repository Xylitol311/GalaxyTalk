package com.example.match.service;

import com.example.match.domain.MatchResultStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_KEY_PREFIX = "user:";
    private static final String MATCH_KEY_PREFIX = "match:";

    /**
     * 유저 상태를 Redis에 저장.
     *
     * @param user 저장할 유저 정보
     */
    public void saveUserStatus(UserMatchStatus user) {
        log.info("save user status");
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);
    }

    /**
     * Redis에서 유저 상태를 조회.
     *
     * @param userId 조회할 유저의 ID
     * @return 유저 상태 (없으면 null)
     */
    public UserMatchStatus getUserStatus(String userId) {
        log.info("Getting user match status...");
        Object data = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        if (data == null) {
            return null;
        }
        if (!(data instanceof UserMatchStatus userMatchStatus)) {
            throw new BusinessException(ErrorCode.REDIS_DATA_MISMATCH,
                    "Redis에서 가져온 데이터가 UserMatchStatus 타입이 아닙니다. userId=" + userId);
        }
        return userMatchStatus;
    }

    /**
     * Redis에서 유저 상태를 삭제.
     *
     * @param userId 삭제할 유저의 ID
     */
    public void deleteUserStatus(String userId) {
        log.info("Deleting user match status...");
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }

    /**
     * 유저의 매칭 큐 목록(셋)에 특정 MBTI 큐를 추가.
     *
     * @param userId   유저 ID
     * @param mbtiName 추가할 MBTI 큐 이름
     */
    public void addUserToQueue(String userId, String mbtiName) {
        redisTemplate.opsForSet().add("user_matching_queues:" + userId, mbtiName);
    }

    /**
     * 유저의 매칭 큐 목록(셋)에서 특정 MBTI 큐를 제거.
     *
     * @param userId   유저 ID
     * @param mbtiName 제거할 MBTI 큐 이름
     */
    public void removeUserFromQueue(String userId, String mbtiName) {
        redisTemplate.opsForSet().remove("user_matching_queues:" + userId, mbtiName);
    }

    /**
     * 유저가 특정 MBTI 큐에 속해있는지 확인.
     *
     * @param userId   유저 ID
     * @param mbtiName 확인할 MBTI 큐 이름
     * @return 속해있으면 true, 아니면 false
     */
    public boolean isUserInQueue(String userId, String mbtiName) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("user_matching_queues:" + userId, mbtiName));
    }

    /**
     * 유저의 매칭 큐 목록(셋)을 조회.
     *
     * @param userId 대상 유저 ID
     * @return 매칭 큐 목록
     */
    public Set<String> getUserMatchingQueues(String userId) {
        Set<Object> resultSet = redisTemplate.opsForSet().members("user_matching_queues:" + userId);
        if (resultSet == null) {
            return Collections.emptySet(); // null 방지를 위해 빈 Set 반환
        }
        return resultSet.stream()
                .map(Object::toString) // Object → String 변환
                .collect(Collectors.toSet());
    }

    /**
     * 매칭 정보를 Redis에 저장.
     *
     * @param matchId     매칭 ID
     * @param matchResult 매칭 결과 상태
     */
    public void saveMatchInfo(String matchId, MatchResultStatus matchResult) {
        log.info("Setting match info to " + matchResult);
        if (matchId == null || matchResult == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "matchId 혹은 matchResult가 null입니다.");
        }
        redisTemplate.opsForValue().set(MATCH_KEY_PREFIX + matchId, matchResult);
    }

    /**
     * Redis에서 매칭 정보를 조회.
     *
     * @param matchId 조회할 매칭 ID
     * @return 매칭 결과 상태 (없으면 null)
     */
    public MatchResultStatus getMatchInfo(String matchId) {
        log.info("Getting match info...");
        Object data = redisTemplate.opsForValue().get(MATCH_KEY_PREFIX + matchId);
        if (data == null) {
            return null;
        }
        if (!(data instanceof MatchResultStatus matchResult)) {
            throw new BusinessException(ErrorCode.REDIS_DATA_MISMATCH,
                    "Redis에서 가져온 데이터가 MatchResultStatus 타입이 아닙니다. matchId=" + matchId);
        }
        return matchResult;
    }

    /**
     * Redis에서 매칭 정보를 삭제.
     *
     * @param matchId 삭제할 매칭 ID
     */
    public void deleteMatchInfo(String matchId) {
        log.info("Deleting match info...");
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }

    /**
     * 실시간 매칭 대기 유저를 관리하기 위해 Sorted Set에 유저를 추가.
     *
     * @param userMatchStatus 추가할 유저 상태
     */
    public void addUserToWaitingQueue(UserMatchStatus userMatchStatus) {
        log.info("Adding user to waiting queue from Redis...");
        if (userMatchStatus == null || userMatchStatus.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForZSet().add("waiting_users", userMatchStatus.getUserId(), System.currentTimeMillis());
    }

    /**
     * Sorted Set에서 매칭 대기 유저를 제거.
     *
     * @param userId 제거할 유저 ID
     */
    public void removeUserFromWaitingQueue(String userId) {
        log.info("Removing user from waiting queue from Redis...");
        redisTemplate.opsForZSet().remove("waiting_users", userId);
    }

    /**
     * Sorted Set에서 랜덤으로 대기 유저 목록을 조회.
     *
     * @param count 조회할 유저 수
     * @return 랜덤 대기 유저 ID 목록
     */
    public List<String> getRandomWaitingUsers(int count) {
        log.info("Getting random waiting users...");
        List<Object> randomObjects = redisTemplate.opsForZSet().randomMembers("waiting_users", count);
        if (randomObjects == null) {
            return List.of();
        }
        return randomObjects.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }
}