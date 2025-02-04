package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.UserMatchStatus;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@Getter
@Component
@RequiredArgsConstructor
public class MatchingQueueManager {
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
     */
    public void addToQueue(UserMatchStatus user) {
        try {
            // 자신의 MBTI 큐에 추가
            MBTI userMbti = MBTI.valueOf(user.getMbti());
            mbtiQueues.get(userMbti).offer(user);

            // 선호하는 MBTI 큐에 추가
            if (user.getPreferredMbti() != null) {
                MBTI preferredMbti = MBTI.valueOf(user.getPreferredMbti());
                mbtiQueues.get(preferredMbti).offer(user);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid MBTI value");
        }
    }

    /**
     * 특정 MBTI 큐에서 배치 사이즈만큼의 유저를 가져옴
     */
    public List<UserMatchStatus> getBatchFromQueue(MBTI mbti) {
        Queue<UserMatchStatus> queue = mbtiQueues.get(mbti);
        List<UserMatchStatus> batch = new ArrayList<>();

        for (int i = 0; i < BATCH_SIZE && !queue.isEmpty(); i++) {
            UserMatchStatus user = queue.poll();
            if (user != null) {
                batch.add(user);
            }
        }

        return batch;
    }

    /**
     * 매칭이 어려운 유저를 연관된 MBTI 큐로 이동
     */
    public void moveToRelatedQueue(UserMatchStatus user) {
        MBTI userMbti = MBTI.valueOf(user.getMbti());
        Queue<UserMatchStatus> largestQueue = findLargestRelatedQueue(userMbti);

        if (largestQueue != null) {
            largestQueue.offer(user);
        }
    }

    /**
     * 연관된 MBTI 큐 중 가장 큰 큐를 찾음
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
}