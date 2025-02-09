package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@Getter
@Component
@RequiredArgsConstructor
public class MatchingQueueManager {
    // Lazy Deletion과 GC 처리
    private final RedisService redisService;

    // MBTI별 매칭 큐
    private final Map<MBTI, Queue<UserMatchStatus>> mbtiQueues = new EnumMap<>(MBTI.class);

    // 큐 크기 상수
    private static final int BATCH_SIZE = 50;

    // MBTI 연관 관계 매핑
    private static final Map<MBTI, List<String>> RELATED_MBTI_PATTERNS = new EnumMap<>(MBTI.class);

    @PostConstruct
    public void init() {
        // MBTI별 큐 초기화 - PriorityBlockingQueue 사용하여 대기 시간 기준 정렬
        for (MBTI mbti : MBTI.values()) {
            mbtiQueues.put(mbti, new PriorityBlockingQueue<>(
                    11,
                    Comparator.comparingLong(UserMatchStatus::getStartTime)
            ));
        }

        // MBTI 연관 관계 초기화
        initializeRelatedMbtiPatterns();
    }

    private void initializeRelatedMbtiPatterns() {
        for (MBTI mbti : MBTI.values()) {
            String mbtiStr = mbti.name();
            List<String> patterns = new ArrayList<>();

            // INFx 패턴
            patterns.add(mbtiStr.substring(0, 3) + ".");
            // INxP 패턴
            patterns.add(mbtiStr.substring(0, 2) + "." + mbtiStr.charAt(3));
            // IxFP 패턴
            patterns.add(mbtiStr.charAt(0) + "." + mbtiStr.substring(2));
            // xNFP 패턴
            patterns.add("." + mbtiStr.substring(1));

            RELATED_MBTI_PATTERNS.put(mbti, patterns);
        }
    }

    /**
     * 유저를 해당하는 MBTI 큐에 추가
     * - 중복 삽입을 막기 위해 user.machingQueues 확인 후 추가
     * - user.machingQueues에 큐 이름(MBTI.name())을 저장
     * - Redis에 변경 사항을 다시 저장하여 상태 동기화
     */
    public void addToQueue(UserMatchStatus user) {
        try {
            // machingQueues가 null일 수 있으므로 초기화
            if (user.getMachingQueues() == null) {
                user.setMachingQueues(new ArrayList<>());
            }

            // 자신의 MBTI 큐에 추가
            MBTI userMbti = MBTI.valueOf(user.getMbti());
            String userMbtiName = userMbti.name();
            // 이미 들어가있는 큐인지 확인
            if (!user.getMachingQueues().contains(userMbtiName)) {
                mbtiQueues.get(userMbti).offer(user);
                user.getMachingQueues().add(userMbtiName);
            }

            // 선호하는 MBTI 큐에 추가
            if (user.getPreferredMbti() != null) {
                MBTI preferredMbti = MBTI.valueOf(user.getPreferredMbti());
                String preferredMbtiName = preferredMbti.name();
                // 이미 들어가있는 큐인지 확인
                if (!user.getMachingQueues().contains(preferredMbtiName)) {
                    mbtiQueues.get(preferredMbti).offer(user);
                    user.getMachingQueues().add(preferredMbtiName);
                }
            }

            // 변경된 user 상태를 Redis에 다시 저장하여 동기화
            redisService.saveUserStatus(user);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_MBTI, "Invalid MBTI value in addToQueue()");
        }
    }

    /**
     * 특정 MBTI 큐에서 배치 사이즈만큼의 유저를 가져옴
     * - Lazy Deletion 정책 때문에, poll된 유저가 Redis에서 삭제되었거나
     *   매칭 상태가 WAITING이 아닐 경우는 후속 로직(필터링)에서 제외될 수 있음
     *   (=매칭되면 안되는 유저이므로 매칭 과정에서 제외)
     */
    public List<UserMatchStatus> getBatchFromQueue(MBTI mbti) {
        Queue<UserMatchStatus> queue = mbtiQueues.get(mbti);
        List<UserMatchStatus> batch = new ArrayList<>();

        for (int i = 0; i < BATCH_SIZE && !queue.isEmpty(); i++) {
            UserMatchStatus user = queue.poll();
            if (user != null) {
                // 여기서 바로 Redis 조회를 하여 "유효한" 유저만 batch에 추가할 수도 있지만,
                // processMatching() 단계에서 filterAvailableUsers()로 중복 확인하므로 일단 add.
                batch.add(user);
            }
        }

        return batch;
    }

    /**
     * 매칭이 어려운 유저를 연관된 MBTI 큐로 이동
     * - 기존 큐에서 제거하지 않음 (Lazy Deletion 및 중복 삽입 관리 고려)
     * - 연관 큐에만 추가하여 매칭 기회를 확대
     */
    public void moveToRelatedQueue(UserMatchStatus user) {
        MBTI userMbti = MBTI.valueOf(user.getMbti());
        Queue<UserMatchStatus> largestQueue = findLargestRelatedQueue(userMbti);

        if (largestQueue != null) {
            largestQueue.offer(user);
        }
    }

    /**
     * 연관된 MBTI 큐 중 사이즈가 가장 큰 큐를 찾음(매칭 확률을 증가시키기 위해)
     */
    private Queue<UserMatchStatus> findLargestRelatedQueue(MBTI mbti) {
        List<String> patterns = RELATED_MBTI_PATTERNS.get(mbti);

        return patterns.stream()
                .flatMap(pattern -> mbtiQueues.entrySet().stream()
                        .filter(entry -> entry.getKey().name().matches(pattern))
                        .map(Map.Entry::getValue))
                .max(Comparator.comparingInt(Queue::size))
                .orElse(null);
    }

    /**
     * 특정 큐에서 유저 제거 (필요한 경우에만)
     * - 사용되는 곳: 매칭 시 유저가 null이거나 유효하지 않을 때 제거
     */
    public void removeFromQueueIfInvalid(MBTI mbti, UserMatchStatus user) {
        Queue<UserMatchStatus> queue = mbtiQueues.get(mbti);
        if (queue != null) {
            queue.removeIf(queuedUser ->
                    queuedUser.getUserId().equals(user.getUserId()));
        }
    }

    /**
     * 특정 MBTI 큐의 크기 반환
     */
    public int getQueueSize(MBTI mbti) {
        return mbtiQueues.get(mbti).size();
    }

    /**
     * 주기적으로 큐에서 "유령 유저(Ghost Users)"를 제거하는 Garbage Collection
     * - fixedDelay = 60000 -> 1분마다 실행 (예시)
     * - Redis에서 이미 삭제된(유효하지 않은) 유저는 큐에서 제거
     * - Lazy Deletion으로 인해 큐에 남아 있을 수 있는 데이터를 주기적으로 정리
     */
    @Scheduled(fixedDelay = 60000)
    public void garbageCollectQueues() {
        for (Map.Entry<MBTI, Queue<UserMatchStatus>> entry : mbtiQueues.entrySet()) {
            Queue<UserMatchStatus> queue = entry.getValue();

            queue.removeIf(user -> {
                UserMatchStatus statusInRedis = redisService.getUserStatus(user.getUserId());
                // Redis에 정보가 없으면 삭제된 유저로 간주
                return (statusInRedis == null || statusInRedis.getStatus() != MatchStatus.WAITING);
            });
        }
    }
}