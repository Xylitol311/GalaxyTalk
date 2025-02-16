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
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_KEY_PREFIX = "user:";
    private static final String MATCH_KEY_PREFIX = "match:";

    // Sorted Set keys for waiting and in-progress 유저 관리
    private static final String WAITING_USERS_KEY = "waiting_users";
    private static final String IN_PROGRESS_USERS_KEY = "inprogress_users";


    /**
     * WAITING 상태인 유저를 원자적으로 IN_PROGRESS 상태로 전환하는 메서드
     * 이 Lua 스크립트는 해당 유저의 JSON 데이터를 가져와서, 상태가 "WAITING"이면 "IN_PROGRESS"로 변경한 후 다시 저장합니다.
     * 원자적 연산을 보장하기 때문에, 여러 배치에서 동시에 같은 유저를 선택하지 못하게 합니다.
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
     * 유저 상태를 Redis에 저장.
     *
     */
    public void saveUserStatus(UserMatchStatus user) {
        log.info("유저 상태 저장: {}", user.getUserId());
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
     */
    public void removeUserFromQueue(String userId, String mbtiName) {
        redisTemplate.opsForSet().remove("user_matching_queues:" + userId, mbtiName);
    }

    /**
     * 유저가 특정 MBTI 큐에 속해있는지 확인.
     *
     */
    public boolean isUserInQueue(String userId, String queueName) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("user_matching_queues:" + userId, queueName));
    }

//    /**
//     * 유저의 매칭 큐 목록(셋)을 조회.
//     *
//     */
//    public Set<String> getUserMatchingQueues(String userId) {
//        Set<Object> resultSet = redisTemplate.opsForSet().members("user_matching_queues:" + userId);
//        if (resultSet == null) {
//            return Collections.emptySet(); // null 방지를 위해 빈 Set 반환
//        }
//        return resultSet.stream()
//                .map(Object::toString) // Object → String 변환
//                .collect(Collectors.toSet());
//    }

    /**
     * 매칭 정보를 Redis에 저장.
     *
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
     * Redis에서 매칭 정보를 삭제.
     */
    public void deleteMatchInfo(String matchId) {
        log.info("매칭 정보 삭제: {}", matchId);
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }

    /**
     * 대기 유저 Sorted Set 관리: 유저 ID와 매칭 시작 시간을 score로 사용
     */
    public void addUserToWaitingQueue(UserMatchStatus userMatchStatus) {
        log.info("대기 큐에 유저 추가: {}", userMatchStatus.getUserId());
        if (userMatchStatus == null || userMatchStatus.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "UserMatchStatus 혹은 userId가 null입니다.");
        }
        redisTemplate.opsForZSet().add(WAITING_USERS_KEY, userMatchStatus.getUserId(), System.currentTimeMillis());
    }


    /**
     * Sorted Set에서 매칭 대기 유저를 제거.
     *
     */
    public void removeUserFromWaitingQueue(String userId) {
        log.info("대기 큐에서 유저 제거: {}", userId);
        redisTemplate.opsForZSet().remove(WAITING_USERS_KEY, userId);
    }

//    /**
//     * in-progress 유저 Sorted Set 추가
//     * - 매칭 작업 중인 유저를 1분 이상 체크하기 위함
//     */
//    public void addUserToInProgressQueue(String userId, long timestamp) {
//        log.info("in-progress 큐에 유저 추가: {}", userId);
//        redisTemplate.opsForZSet().add(IN_PROGRESS_USERS_KEY, userId, timestamp);
//    }
//    /**
//     * in-progress 유저 Sorted Set에서 조회
//     * - 작업 상태의 유저 id 반환
//     */
//    public Set<String> getInProgressUsersByScore(double maxScore) {
//        Set<Object> result = redisTemplate.opsForZSet().rangeByScore(IN_PROGRESS_USERS_KEY, 0, maxScore);
//        if (result == null) return Collections.emptySet();
//        return result.stream().map(Object::toString).collect(Collectors.toSet());
//    }
//    /**
//     * in-progress 유저 Sorted Set 관리
//     * -  매칭 작업 중인 유저를 1분 이상 체크하기 위함
//     */
//    public void removeUserFromInProgressQueue(String userId) {
//        log.info("in-progress 큐에서 유저 제거: {}", userId);
//        redisTemplate.opsForZSet().remove(IN_PROGRESS_USERS_KEY, userId);
//    }

    /**
     * Redis Sorted Set에서 특정 score 이하의 대기 유저 조회 (예: 5분 이상 대기한 유저)
     */
    public Set<String> getWaitingUsersByScore(double maxScore) {
        Set<Object> result = redisTemplate.opsForZSet().rangeByScore(WAITING_USERS_KEY, 0, maxScore);
        if (result == null) return Collections.emptySet();
        return result.stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * Sorted Set에서 랜덤으로 대기 유저 목록을 조회.
     */
    public List<String> getRandomWaitingUsers(int count) {
        log.info("랜덤 대기 유저 조회");
        List<Object> randomObjects = redisTemplate.opsForZSet().randomMembers(WAITING_USERS_KEY, count);
        if (randomObjects == null) {
            return List.of();
        }
        return randomObjects.stream().map(Object::toString).collect(Collectors.toList());
    }
}