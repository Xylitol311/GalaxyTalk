package com.example.match.service;

import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
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
    // in-memory에서 각 userId의 매칭 시작 시간(startTime)을 저장하여 우선순위 큐의 정렬 기준으로 사용.
    private final Map<String, Long> userStartTimeMap = new ConcurrentHashMap<>();
    // [신규 코드] 매칭 큐를 단 하나만 사용.
    private final Queue<String> universalQueue = new PriorityBlockingQueue<>(
            11,
            Comparator.comparingLong(
                    userId -> userStartTimeMap.getOrDefault(userId, Long.MAX_VALUE)
            )
    );
//    private final Map<MBTI, Queue<String>> mbtiQueues = new EnumMap<>(MBTI.class);
//    private static final Map<MBTI, List<String>> RELATED_MBTI_PATTERNS = new EnumMap<>(MBTI.class);
    private static final int BATCH_SIZE = 50;

    /**
     * 초기화: 각 MBTI별 매칭 큐를 초기화하고, 우선순위 큐의 정렬 기준(매칭 시작 시간)을 위해 userStartTimeMap을 참조.
     * + 연관 MBTI 패턴을 초기화.
     */
//    @PostConstruct
//    public void init() {
//        for (MBTI mbti : MBTI.values()) {
//            // PriorityBlockingQueue를 사용, userStartTimeMap에 저장된 시작 시간을 기준으로 정렬.
//            mbtiQueues.put(mbti, new PriorityBlockingQueue<>(
//                    11,
//                    Comparator.comparingLong(userId -> userStartTimeMap.getOrDefault(userId, Long.MAX_VALUE))
//            ));
//        }
//        initializeRelatedMbtiPatterns();
//    }

    /**
     * 각 MBTI에 대해 연관된 MBTI 패턴(예: INFx, INxP, IxFP, xNFP)을 초기화.
     */
//    private void initializeRelatedMbtiPatterns() {
//        for (MBTI mbti : MBTI.values()) {
//            String mbtiStr = mbti.name();
//            List<String> patterns = new ArrayList<>();
//            // 예시: "INFx" 패턴
//            patterns.add(mbtiStr.substring(0, 3) + ".");
//            // 예시: "INxP" 패턴
//            patterns.add(mbtiStr.substring(0, 2) + "." + mbtiStr.charAt(3));
//            // 예시: "IxFP" 패턴
//            patterns.add(mbtiStr.charAt(0) + "." + mbtiStr.substring(2));
//            // 예시: "xNFP" 패턴
//            patterns.add("." + mbtiStr.substring(1));
//            RELATED_MBTI_PATTERNS.put(mbti, patterns);
//        }
//    }

    /**
     * 유저를 해당 MBTI 큐에 추가.
     * - 큐에 추가할 때 Redis의 유저 매칭 큐 목록(셋)도 업데이트.
     * - Redis에 저장된 유저 객체의 matchingQueues 필드를 최신 상태로 동기화
     *
     * @param user 매칭 큐에 추가할 유저 정보
     */
    public void addToQueue(UserMatchStatus user) {
        log.info("add to Matching queue");
        try {
            String userId = user.getUserId();
//            // 자신의 MBTI 큐에 추가
//            MBTI userMbti = MBTI.valueOf(user.getMbti());
//            String userMbtiName = userMbti.name();
//            // Redis의 유저 큐 목록에서 해당 MBTI 큐에 속해있는지 확인
//            if (!redisService.isUserInQueue(userId, userMbtiName)) {
//                // in-memory 큐에 userId 추가
//                mbtiQueues.get(userMbti).offer(userId);
//                // 매칭 시작 시간을 함께 저장 (우선순위 큐 정렬 기준)
//                userStartTimeMap.put(userId, user.getStartTime());
//                // Redis의 유저 매칭 큐 목록(셋)에 해당 MBTI 큐 추가
//                redisService.addUserToQueue(userId, userMbtiName);
//            }
//
//            // 선호하는 MBTI가 있는 경우 해당 큐에도 추가
//            if (user.getPreferredMbti() != null) {
//                MBTI preferredMbti = MBTI.valueOf(user.getPreferredMbti());
//                String preferredMbtiName = preferredMbti.name();
//                if (!redisService.isUserInQueue(userId, preferredMbtiName)) {
//                    mbtiQueues.get(preferredMbti).offer(userId);
//                    userStartTimeMap.put(userId, user.getStartTime());
//                    redisService.addUserToQueue(userId, preferredMbtiName);
//                }
//            }
//
//            // 최신 매칭 큐 목록을 Redis에서 조회하여 유저 객체의 matchingQueues 필드를 업데이트.
//            Set<String> queues = redisService.getUserMatchingQueues(userId);
//            user.setMachingQueues(new ArrayList<>(queues));
//            redisService.saveUserStatus(user);

            // [신규] universalQueue에만 추가
            // 중복 확인(이미 universalQueue에 들어있으면 추가 X) 로직은 간단히 "contains"로 처리
            if (!universalQueue.contains(userId)) {
                universalQueue.offer(userId);
                userStartTimeMap.put(userId, user.getStartTime());
            }

            // Redis에 저장된 user_matching_queues 는 사용하지 않거나,
            // "SINGLE_QUEUE" 라는 가상의 키로 단일 관리
            if (!redisService.isUserInQueue(userId, "SINGLE_QUEUE")) {
                redisService.addUserToQueue(userId, "SINGLE_QUEUE");
            }

            // user 객체 업데이트 후 Redis에 저장
            // matchingQueues에는 "SINGLE_QUEUE"만 들어있게 할 수 있음
            user.setMachingQueues(new ArrayList<>(Collections.singletonList("SINGLE_QUEUE")));
            redisService.saveUserStatus(user);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_MBTI, "Invalid MBTI value in addToQueue()");
        }
    }

    /**
     * 특정 MBTI 큐에서 배치 사이즈(BATCH_SIZE)만큼의 유저를 꺼내어 반환.
     * - 큐에는 poll 후 Redis에서 유저 상태를 조회하여 UserMatchStatus 객체를 반환 받음
     * - 동시에 Redis의 유저 매칭 큐 목록에서 해당 MBTI 큐 정보를 제거.
     * </p>
     *
     * @return 매칭 처리할 유저 목록
     */
//    public List<UserMatchStatus> getBatchFromQueue(MBTI mbti) {
    public List<UserMatchStatus> getBatchFromQueue() {
        log.info("get batch from Matching queue");
//        Queue<String> queue = mbtiQueues.get(mbti);
        List<UserMatchStatus> batch = new ArrayList<>();

//        if (queue.size() == 1) {
//            String userId = queue.peek();
//            UserMatchStatus user = redisService.getUserStatus(userId);
//            moveToRelatedQueue(user, mbti);
//            return batch;
//        }

//        for (int i = 0; i < BATCH_SIZE && !queue.isEmpty(); i++) {
//            String userId = queue.poll();
//            if (userId != null) {
//                // in-memory startTimeMap에서 해당 userId 제거
//                userStartTimeMap.remove(userId);
//                // Redis의 유저 매칭 큐 목록에서도 해당 MBTI 큐 정보 제거
//                redisService.removeUserFromQueue(userId, mbti.name());
//                // Redis에서 최신 유저 상태 조회
//                UserMatchStatus user = redisService.getUserStatus(userId);
//                if (user != null) {
//                    batch.add(user);
//                }
//            }
//        }
        if (universalQueue.size() < 2) {
            return batch;
        }

        for (int i = 0; i < BATCH_SIZE && !universalQueue.isEmpty(); i++) {
            String userId = universalQueue.poll();
            if (userId == null) continue;

            // Redis에서 최신 상태 조회
            UserMatchStatus user = redisService.getUserStatus(userId);
            if (user != null && user.getStatus() == MatchStatus.WAITING) {
                batch.add(user);
            } else {
                // 유효하지 않으면 무시
            }
        }

        return batch;
    }

    /**
     * 매칭이 어려운 유저를 연관된 MBTI 큐로 이동
     * - 이동 시 매칭 큐 정보 업데이트.
     *
     * @param user 이동할 유저 정보
     * @param mbti 현재 속한 MBTI
     */
//    public void moveToRelatedQueue(UserMatchStatus user, MBTI mbti) {
//        log.info("move to another Matching queue");
//        // 연관 MBTI 큐 중에서 큐 사이즈가 가장 큰 MBTI를 선택.
//        MBTI targetMbti = findLargestRelatedQueue(mbti);
//        if (targetMbti != null) {
//            // 선택된 대상 MBTI 큐 획득
//            Queue<String> targetQueue = mbtiQueues.get(targetMbti);
//            // in-memory 큐에 userId 추가 및 시작 시간 업데이트
//            targetQueue.offer(user.getUserId());
//            userStartTimeMap.put(user.getUserId(), user.getStartTime());
//            // Redis의 유저 매칭 큐 목록에도 대상 MBTI 큐 추가
//            redisService.addUserToQueue(user.getUserId(), targetMbti.name());
//            // 최신 큐 목록을 반영하여 유저 객체 업데이트 후 Redis 저장
//            Set<String> queues = redisService.getUserMatchingQueues(user.getUserId());
//            user.setMachingQueues(new ArrayList<>(queues));
//            redisService.saveUserStatus(user);
//        }
//    }

    /**
     * 연관된 MBTI 큐 중에서 큐 사이즈가 가장 큰 MBTI를 탐색
     * - 대상 MBTI 값을 반환하여 이후 Redis 업데이트에 활용
     *
     * @param mbti 기준 MBTI
     * @return 큐 사이즈가 가장 큰 연관 MBTI (없으면 null)
     */
//    private MBTI findLargestRelatedQueue(MBTI mbti) {
//        log.info("find largest size Matching queue");
//        List<String> patterns = RELATED_MBTI_PATTERNS.get(mbti);
//        return mbtiQueues.entrySet().stream()
//                .filter(entry ->
//                        patterns.stream().anyMatch(pattern -> entry.getKey().name().matches(pattern))
//                )
//                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
//                .map(Map.Entry::getKey)
//                .orElse(null);
//    }

    /**
     * 특정 MBTI 큐의 크기를 반환.
     *
     * @param mbti 대상 MBTI
     * @return 큐의 크기
     */
//    public int getQueueSize(MBTI mbti) {
//        log.info("get Matching queue size");
//        return mbtiQueues.get(mbti).size();
//    }

    /**
     * 주기적으로 매칭 큐에서 유령 유저(더 이상 유효하지 않은 유저)를 제거.
     * - in-memory 큐에 저장된 userId 기준으로 Redis에서 유저 상태를 재확인.
     *   유효하지 않은 경우 해당 userId를 큐와 userStartTimeMap, 그리고 Redis의 매칭 큐 목록에서 제거.
     */
    @Scheduled(fixedDelay = 60000)
    public void garbageCollectQueues() {
//        log.info("garbage collect matching queues");
//        for (Map.Entry<MBTI, Queue<String>> entry : mbtiQueues.entrySet()) {
//            Queue<String> queue = entry.getValue();
//            queue.removeIf(userId -> {
//                UserMatchStatus statusInRedis = redisService.getUserStatus(userId);
//                if (statusInRedis == null || statusInRedis.getStatus() != MatchStatus.WAITING) {
//                    // in-memory startTimeMap에서 제거
//                    userStartTimeMap.remove(userId);
//                    // Redis의 해당 MBTI 큐 정보에서도 제거
//                    redisService.removeUserFromQueue(userId, entry.getKey().name());
//                    return true;
//                }
//                return false;
//            });
//        }

        log.info("garbage collect single matching queue");
        List<String> retained = new ArrayList<>();
        while (!universalQueue.isEmpty()) {
            String userId = universalQueue.poll();
            UserMatchStatus statusInRedis = redisService.getUserStatus(userId);
            if (statusInRedis != null && statusInRedis.getStatus() == MatchStatus.WAITING) {
                // 여전히 WAITING이면 유지
                retained.add(userId);
            } else {
                // 매칭 완료 or 취소된 유저 제거
                userStartTimeMap.remove(userId);
                redisService.removeUserFromQueue(userId, "SINGLE_QUEUE");
            }
        }
        // 다시 큐에 삽입
        for (String userId : retained) {
            universalQueue.offer(userId);
        }
    }
}