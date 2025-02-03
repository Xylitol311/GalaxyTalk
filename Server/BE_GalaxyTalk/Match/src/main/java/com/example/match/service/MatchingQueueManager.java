package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.UserMatchStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
public class MatchingQueueManager {
    // MBTI별 매칭 큐
    @Getter
    private final Map<MBTI, Queue<UserMatchStatus>> mbtiQueues = new EnumMap<>(MBTI.class);

    // 대기 시간이 긴 유저들을 위한 큐
    @Getter
    private final Queue<UserMatchStatus> longWaitQueue = new ConcurrentLinkedQueue<>();

    // 큐 크기 상수
    private static final int BATCH_SIZE = 10;
    private static final long MAX_WAIT_TIME = 30000; // 30초

    @PostConstruct
    public void init() {
        // MBTI별 큐 초기화
        for (MBTI mbti : MBTI.values()) {
            mbtiQueues.put(mbti, new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * 유저를 해당하는 MBTI 큐에 추가
     */
    public void addToQueue(UserMatchStatus user) {
        try {
            MBTI preferredMbti = MBTI.valueOf(user.getPreferredMbti());
            mbtiQueues.get(preferredMbti).offer(user);
        } catch (IllegalArgumentException e) {
            // MBTI 형식이 잘못된 경우 롱타임 큐에 추가
            longWaitQueue.offer(user);
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
     * 대기 시간이 긴 유저들을 롱타임 큐로 이동
     */
    public void moveToLongWaitQueue(UserMatchStatus user) {
        long waitTime = System.currentTimeMillis() - user.getStartTime();
        if (waitTime > MAX_WAIT_TIME) {
            // 원래 있던 MBTI 큐에서 제거
            MBTI preferredMbti = MBTI.valueOf(user.getPreferredMbti());
            mbtiQueues.get(preferredMbti).remove(user);
            // 롱타임 큐에 추가
            longWaitQueue.offer(user);
        }
    }

    /**
     * 롱타임 큐에서 배치 사이즈만큼의 유저를 가져옴
     */
    public List<UserMatchStatus> getBatchFromLongWaitQueue() {
        List<UserMatchStatus> batch = new ArrayList<>();

        for (int i = 0; i < BATCH_SIZE && !longWaitQueue.isEmpty(); i++) {
            UserMatchStatus user = longWaitQueue.poll();
            if (user != null) {
                batch.add(user);
            }
        }

        return batch;
    }
}