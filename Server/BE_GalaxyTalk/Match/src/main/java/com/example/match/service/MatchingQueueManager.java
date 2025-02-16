package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class MatchingQueueManager {
    private final RedisService redisService;

    // MBTI별 매칭 큐: 각 큐는 PriorityBlockingQueue로, 매칭 시작 시간을 기준으로 정렬
    private final Map<MBTI, Queue<String>> mbtiQueues = new EnumMap<>(MBTI.class);

    // in-memory에서 각 userId의 매칭 시작 시간을 저장 (정렬 기준)
    private final Map<String, Long> userStartTimeMap = new ConcurrentHashMap<>();

    // 관련 MBTI 정보를 저장할 맵
    private final Map<MBTI, List<MBTI>> relatedMbtiMap = new EnumMap<>(MBTI.class);

    private static final int BATCH_SIZE = 50;

    /**
     * 초기화: 각 MBTI별 매칭 큐를 초기화하고, 우선순위 큐의 정렬 기준(매칭 시작 시간)을 위해 userStartTimeMap을 참조.
     * + 연관 MBTI 패턴을 초기화.
     */
    @PostConstruct
    public void init() {
        // 모든 MBTI에 대해 큐 초기화
        for (MBTI mbti : MBTI.values()) {
            mbtiQueues.put(mbti, new PriorityBlockingQueue<>(11, Comparator.comparingLong(
                    userId -> userStartTimeMap.getOrDefault(userId, Long.MAX_VALUE)
            )));
        }
        // 각 MBTI에 대해 3글자 이상 일치하는 패턴 기반 관련 MBTI 큐 초기화
        initializeRelatedMbtiPatterns();
    }

    /**
     * 각 MBTI에 대해 관련 MBTI 패턴을 생성하고,
     * - 다른 MBTI의 이름이 해당 패턴과 일치하면 관련 큐로 추가합니다.
     */
    private void initializeRelatedMbtiPatterns() {
        for (MBTI mbti : MBTI.values()) {
            String mbtiStr = mbti.name();
            List<String> patterns = new ArrayList<>();
            patterns.add(mbtiStr.substring(0, 3) + ".");          // 예: INF.
            patterns.add(mbtiStr.substring(0, 2) + "." + mbtiStr.charAt(3)); // 예: INxP
            patterns.add(mbtiStr.charAt(0) + "." + mbtiStr.substring(2));      // 예: I.NP
            patterns.add("." + mbtiStr.substring(1));              // 예: .NFP

            List<MBTI> relatedList = new ArrayList<>();
            // 다른 MBTI 중 이름이 3글자 이상 일치하는지 검사
            for (MBTI other : MBTI.values()) {
                if (other.equals(mbti)) continue;
                String otherStr = other.name();
                // 예시로, 앞 3글자가 동일하면 관련 있다고 판단 (필요에 따라 regex 등으로 세분화 가능)
                if (otherStr.substring(0, 3).equals(mbtiStr.substring(0, 3))) {
                    relatedList.add(other);
                }
            }
            relatedMbtiMap.put(mbti, relatedList);
        }
    }

//    /**
//     * 특정 MBTI 큐의 후보 userId 리스트를 반환.
//     * - 큐에서 모든 userId를 가져와서, 중복 없이 후보 리스트로 반환.
//     */
//    public List<String> getCandidateIdsFromQueue(MBTI mbti) {
//        Queue<String> queue = mbtiQueues.get(mbti);
//        if (queue == null) return Collections.emptyList();
//        // 반환 전에 현재 큐 상태를 복사하여 처리
//        return new ArrayList<>(queue);
//    }

    /**
     * 사용자를 자신의 MBTI 큐 및 (선호하는 MBTI가 있다면) 해당 큐에도 추가
     * 중복 추가를 방지하기 위해 Redis와 in-memory를 활용하여 관리
     */
    public void addToQueue(UserMatchStatus user) {
        log.info("사용자 {}를 매칭 큐에 추가", user.getUserId());
        String userId = user.getUserId();

        // 본인 MBTI 큐 추가
        MBTI userMbti = MBTI.valueOf(user.getMbti());
        addUserToSpecificQueue(userId, user.getStartTime(), userMbti);

        // 선호하는 MBTI가 있다면 해당 큐에도 추가 (중복 체크)
        if (user.getPreferredMbti() != null) {
            MBTI preferredMbti = MBTI.valueOf(user.getPreferredMbti());
            if (!preferredMbti.equals(userMbti)) {
                addUserToSpecificQueue(userId, user.getStartTime(), preferredMbti);
            }
        }

        // Redis에 "user_matching_queues:{userId}"에 추가 (큐 목록 관리)
        if (!redisService.isUserInQueue(userId, "MULTI_QUEUE")) {
            redisService.addUserToQueue(userId, "MULTI_QUEUE");
        }
        // user 객체 업데이트: 현재 속한 큐 목록 저장 (예시로 "MULTI_QUEUE"만 사용)
        user.setMatchingQueues(new ArrayList<>(Collections.singletonList("MULTI_QUEUE")));
        redisService.saveUserStatus(user);
    }

    /** 특정 MBTI 큐에 사용자 추가 (중복 여부 체크)
     * – 현재 in-memory 큐에서 contains()를 사용하지만,
     * - 초기 유저수가 많지 않아 지금은 contains로 확인하지만 필요 시 redis 기반으로 전환 가능
     */

    private void addUserToSpecificQueue(String userId, long startTime, MBTI mbti) {
        Queue<String> queue = mbtiQueues.get(mbti);
        if (!queue.contains(userId)) {
            queue.offer(userId);
            userStartTimeMap.put(userId, startTime);
            // Redis에 해당 MBTI 큐 정보 업데이트
            redisService.addUserToQueue(userId, mbti.name());
        }
    }

//    /**
//     * 특정 MBTI 큐에서 배치 단위로 사용자 추출
//     * 만약 큐에 1명만 남은 경우, 관련 큐 중 인원이 많은 큐에 추가하여 매칭 확률을 높임
//     * 단, 기존 큐에서 삭제되지는 않음.
//     */
//    public List<UserMatchStatus> getBatchFromQueue(MBTI mbti) {
//        log.info("{} 큐에서 배치 추출", mbti.name());
//        List<UserMatchStatus> batch = new ArrayList<>();
//        Queue<String> queue = mbtiQueues.get(mbti);
//
//        if (queue == null || queue.isEmpty()) {
//            return batch;
//        }
//
//        // 후보가 1명만 남은 경우 moveToRelatedQueue 실행
//        if (queue.size() == 1) {
//            String loneUserId = queue.peek();
//            moveToRelatedQueue(loneUserId, mbti);
//        }
//        for (int i = 0; i < BATCH_SIZE && !queue.isEmpty(); i++) {
//            String userId = queue.poll();
//            if (userId != null) {
//                userStartTimeMap.remove(userId);
//                redisService.removeUserFromQueue(userId, mbti.name());
//                UserMatchStatus user = redisService.getUserStatus(userId);
//                if (user != null && user.getStatus() == MatchStatus.WAITING) {
//                    batch.add(user);
//                }
//            }
//        }
//        return batch;
//    }

    /**
     * 통합 배치 추출 메서드
     * 지정된 MBTI와 그와 관련된 모든 큐에서 후보를 통합하여 추출합니다.
     */
    public List<UserMatchStatus> getIntegratedBatchFromQueue(MBTI mbti) {
        log.info("통합 배치 추출: {} 및 관련 큐", mbti.name());
        List<UserMatchStatus> integratedBatch = new ArrayList<>();
        // 통합 대상 MBTI 셋 구성: 본인 MBTI + 관련 MBTI
        Set<MBTI> targetMbtiSet = new HashSet<>();
        targetMbtiSet.add(mbti);
        List<MBTI> related = relatedMbtiMap.get(mbti);
        if (related != null) {
            targetMbtiSet.addAll(related);
        }
        // 각 대상 큐에서 후보를 추출
        for (MBTI target : targetMbtiSet) {
            Queue<String> queue = mbtiQueues.get(target);
            if (queue == null || queue.isEmpty()) continue;
            int count = 0;
            int initialSize = queue.size();
            while (count < BATCH_SIZE && !queue.isEmpty()) {
                String userId = queue.poll();
                if (userId != null) {
                    userStartTimeMap.remove(userId);
                    // Redis에서 해당 MBTI 큐 정보 업데이트
                    redisService.removeUserFromQueue(userId, target.name());
                    UserMatchStatus user = redisService.getUserStatus(userId);
                    if (user != null && user.getStatus() == MatchStatus.WAITING) {
                        // 중복 제거: 이미 목록에 추가된 경우 제외
                        boolean exists = integratedBatch.stream().anyMatch(u -> u.getUserId().equals(userId));
                        if (!exists) {
                            integratedBatch.add(user);
                        }
                    }
                }
                count++;
            }
            log.info("{} 큐에서 {}개의 후보를 추출", target.name(), initialSize - queue.size());
        }
        return integratedBatch;
    }

//    /**
//     * 매칭이 어려운 유저를 연관된 MBTI 큐로 이동
//     * - 이동 시 매칭 큐 정보 업데이트.
//     */
//    public void moveToRelatedQueue(String userId, MBTI currentMbti) {
//        log.info("사용자 {}를 {}와 관련된 다른 큐로 추가", userId, currentMbti.name());
//        List<MBTI> related = relatedMbtiMap.get(currentMbti);
//        MBTI targetMbti = null;
//        int maxSize = 0;
//
//        // 연관된 큐 중 가장 수가 많은 곳으로 결정
//        for (MBTI mbti : related) {
//            Queue<String> q = mbtiQueues.get(mbti);
//            if (q.size() > maxSize) {
//                maxSize = q.size();
//                targetMbti = mbti;
//            }
//        }
//        if (targetMbti == null && !related.isEmpty()) {
//            targetMbti = related.get(0);
//        }
//        if (targetMbti != null) {
//            addUserToSpecificQueue(userId, System.currentTimeMillis(), targetMbti);
//        }
//    }



    /**
     * 주기적으로 각 MBTI 큐에서 유효하지 않은 사용자(매칭 완료 또는 취소된 사용자)를 제거
     */
    @Scheduled(fixedDelay = 60000)
    public void garbageCollectQueues() {
        log.info("각 MBTI 큐에 대한 garbage collection 실행");
        for (Map.Entry<MBTI, Queue<String>> entry : mbtiQueues.entrySet()) {
            Queue<String> queue = entry.getValue();
            queue.removeIf(userId -> {
                UserMatchStatus statusInRedis = redisService.getUserStatus(userId);
                if (statusInRedis == null || statusInRedis.getStatus() != MatchStatus.WAITING) {
                    userStartTimeMap.remove(userId);
                    redisService.removeUserFromQueue(userId, entry.getKey().name());
                    return true;
                }
                return false;
            });
        }
    }
}