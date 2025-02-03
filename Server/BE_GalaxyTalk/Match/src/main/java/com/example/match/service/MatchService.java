package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class MatchService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MatchingQueueManager queueManager;
    private final WebClient aiServiceClient;
    private final WebClient chatServiceClient;

    // 매칭 관련 상수
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final long MAX_WAIT_TIME = 30000; // 30초

    /**
     * 매칭 시작 처리
     * 1. 유저 상태 Redis에 저장
     * 2. 선호하는 MBTI 큐에 추가
     * 3. WebSocket으로 대기 시작 알림
     */
    public void startMatching(UserMatchStatus user) {
        // Redis에 유저 상태 저장
        String userKey = "user:" + user.getUserId();
        user.setStatus(MatchStatus.WAITING);
        user.setStartTime(System.currentTimeMillis());
        redisTemplate.opsForValue().set(userKey, user);

        // 매칭 큐에 추가
        queueManager.addToQueue(user);

        // WebSocket으로 대기 상태 알림
        notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");
    }

    /**
     * 각 MBTI 큐별로 매칭 프로세스 실행
     * 5초마다 모든 큐에 대해 비동기로 처리
     */
    @Scheduled(fixedRate = 5000)
    public void processAllQueues() {
        // 모든 MBTI 큐에 대해 비동기 매칭 프로세스 실행
        List<CompletableFuture<Void>> futures = Arrays.stream(MBTI.values())
                .map(this::processMbtiQueue)
                .collect(Collectors.toList());

        // 롱타임 큐 처리
        futures.add(processLongWaitQueue());

        // 모든 비동기 처리 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 특정 MBTI 큐의 매칭 처리를 비동기로 실행
     */
    @Async
    public CompletableFuture<Void> processMbtiQueue(MBTI mbti) {
        List<UserMatchStatus> batch = queueManager.getBatchFromQueue(mbti);
        if (batch.size() < 2) {
            checkAndMoveToLongWaitQueue(batch);
            return CompletableFuture.completedFuture(null);
        }

        processMatching(batch);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 롱타임 큐의 매칭 처리를 비동기로 실행
     */
    @Async
    public CompletableFuture<Void> processLongWaitQueue() {
        List<UserMatchStatus> batch = queueManager.getBatchFromLongWaitQueue();
        if (batch.size() < 2) {
            return CompletableFuture.completedFuture(null);
        }

        processMatching(batch);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 배치 단위로 매칭 처리
     * 가장 높은 유사도를 가진 페어를 찾아 매칭
     */
    private void processMatching(List<UserMatchStatus> users) {
        Map<MatchPair, Double> similarityScores = new HashMap<>();

        // 모든 가능한 페어의 유사도 계산
        for (int i = 0; i < users.size() - 1; i++) {
            for (int j = i + 1; j < users.size(); j++) {
                UserMatchStatus user1 = users.get(i);
                UserMatchStatus user2 = users.get(j);

                double similarity = calculateSimilarity(user1, user2);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    similarityScores.put(new MatchPair(user1, user2), similarity);
                }
            }
        }

        // 유사도가 가장 높은 페어부터 매칭
        similarityScores.entrySet().stream()
                .sorted(Map.Entry.<MatchPair, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    MatchPair pair = entry.getKey();
                    createMatch(pair.user1, pair.user2);
                    users.remove(pair.user1);
                    users.remove(pair.user2);
                });

        // 매칭되지 않은 유저들 처리
        checkAndMoveToLongWaitQueue(users);
    }

    /**
     * 매칭되지 않은 유저들의 대기 시간 체크 후
     * 필요한 경우 롱타임 큐로 이동
     */
    private void checkAndMoveToLongWaitQueue(List<UserMatchStatus> users) {
        for (UserMatchStatus user : users) {
            long waitTime = System.currentTimeMillis() - user.getStartTime();
            if (waitTime > MAX_WAIT_TIME) {
                queueManager.moveToLongWaitQueue(user);
                notifyUser(user.getUserId(), "LONG_WAIT",
                        "매칭 대기 시간이 길어져 더 넓은 범위에서 매칭을 시도합니다.");
            }
        }
    }

    /**
     * AI 서버에 두 유저 간의 유사도 점수 계산 요청
     */
    private double calculateSimilarity(UserMatchStatus user1, UserMatchStatus user2) {
        Map<String, Object> request = Map.of(
                "user1", user1,
                "user2", user2
        );

        return aiServiceClient.post()
                .uri("/similarity") // AI 서버의 엔드포인트
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Double.class)
                .defaultIfEmpty(0.0) // 응답이 없을 경우 기본값 0.0 반환
                .block(); // 동기 처리 (비동기 처리하려면 Mono 반환)
    }

    // 매칭 페어를 관리하기 위한 내부 클래스
    private static class MatchPair {
        private final UserMatchStatus user1;
        private final UserMatchStatus user2;

        public MatchPair(UserMatchStatus user1, UserMatchStatus user2) {
            this.user1 = user1;
            this.user2 = user2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MatchPair matchPair = (MatchPair) o;
            return (Objects.equals(user1, matchPair.user1) &&
                    Objects.equals(user2, matchPair.user2)) ||
                    (Objects.equals(user1, matchPair.user2) &&
                            Objects.equals(user2, matchPair.user1));
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    user1.getUserId().compareTo(user2.getUserId()) < 0 ?
                            user1.getUserId() + user2.getUserId() :
                            user2.getUserId() + user1.getUserId()
            );
        }
    }

    /**
     * 두 유저 간의 매칭 생성
     * 1. 매칭 ID 생성
     * 2. 두 유저의 상태 업데이트
     * 3. 매칭 성사 알림 전송
     */
    private void createMatch(UserMatchStatus user1, UserMatchStatus user2) {
        String matchId = UUID.randomUUID().toString();
        updateMatchStatus(user1, user2, matchId);
        notifyMatch(user1.getUserId(), user2.getUserId(), matchId);
    }

    /**
     * 매칭된 유저들의 상태 정보 업데이트
     * 1. 각 유저의 상태를 MATCHED로 변경
     * 2. 매칭 ID 설정
     * 3. Redis에 업데이트된 정보 저장
     */
    private void updateMatchStatus(UserMatchStatus user1, UserMatchStatus user2, String matchId) {
        user1.setStatus(MatchStatus.MATCHED);
        user2.setStatus(MatchStatus.MATCHED);
        user1.setMatchId(matchId);
        user2.setMatchId(matchId);

        redisTemplate.opsForValue().set("user:" + user1.getUserId(), user1);
        redisTemplate.opsForValue().set("user:" + user2.getUserId(), user2);
        redisTemplate.opsForValue().set("match:" + matchId, List.of(user1.getUserId(), user2.getUserId()));
    }

    /**
     * 매칭 수락/거절 응답 처리
     * 유저의 응답에 따라 수락 또는 거절 프로세스 실행
     */
    public void processMatchResponse(MatchResponse response) {
        String userKey = "user:" + response.getUserId();
        UserMatchStatus user = (UserMatchStatus) redisTemplate.opsForValue().get(userKey);

        if (response.isAccepted()) {
            processAcceptance(user);
        } else {
            processRejection(user);
        }
    }

    /**
     * 매칭 수락 처리
     * 1. 유저의 수락 상태 업데이트
     * 2. 양쪽 모두 수락한 경우 채팅방 생성
     * 3. 매칭 정보 정리
     */
    private void processAcceptance(UserMatchStatus user) {
        user.setAccepted(true);
        redisTemplate.opsForValue().set("user:" + user.getUserId(), user);

        if (checkBothAccepted(user.getMatchId())) {
            createChatRoom(user.getMatchId());
            cleanupMatch(user.getMatchId());
        }
    }

    /**
     * 매칭 거절 처리
     * 1. 상대방 유저 정보 조회
     * 2. 두 유저의 상태 초기화
     * 3. 거절 알림 전송
     */
    private void processRejection(UserMatchStatus user) {
        String matchId = user.getMatchId();
        UserMatchStatus otherUser = findOtherUser(matchId, user.getUserId());

        if (otherUser != null) {
            resetUsers(user, otherUser);
            notifyUser(otherUser.getUserId(), "MATCH_FAILED", "상대방이 매칭을 거절했습니다.");
        }
    }

    /**
     * 매칭된 두 유저가 모두 수락했는지 확인
     * 1. 매칭 ID로 관련된 유저 목록 조회
     * 2. 각 유저의 수락 상태 확인
     * 3. 모든 유저가 수락한 경우에만 true 반환
     */
    private boolean checkBothAccepted(String matchId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue().get("match:" + matchId);
        if (userIds == null) return false;

        return userIds.stream()
                .map(userId -> (UserMatchStatus) redisTemplate.opsForValue().get("user:" + userId))
                .filter(Objects::nonNull)
                .allMatch(UserMatchStatus::isAccepted);
    }

    /**
     * 매칭된 상대방 유저 정보 조회
     */
    private UserMatchStatus findOtherUser(String matchId, String userId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue().get("match:" + matchId);
        if (userIds == null) return null;

        String otherUserId = userIds.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);

        return otherUserId != null ?
                (UserMatchStatus) redisTemplate.opsForValue().get("user:" + otherUserId) : null;
    }

    /**
     * 매칭 실패 시 유저들의 상태 초기화 및 재매칭
     */
    private void resetUsers(UserMatchStatus user1, UserMatchStatus user2) {
        // 상태 초기화
        user1.setStatus(MatchStatus.WAITING);
        user2.setStatus(MatchStatus.WAITING);
        user1.setMatchId(null);
        user2.setMatchId(null);
        user1.setAccepted(false);
        user2.setAccepted(false);

        // 시작 시간 갱신
        user1.setStartTime(System.currentTimeMillis());
        user2.setStartTime(System.currentTimeMillis());

        // Redis 상태 업데이트
        redisTemplate.opsForValue().set("user:" + user1.getUserId(), user1);
        redisTemplate.opsForValue().set("user:" + user2.getUserId(), user2);

        // 다시 큐에 추가
        queueManager.addToQueue(user1);
        queueManager.addToQueue(user2);
    }

    /**
     * 매칭 성공 시 채팅방 생성 요청
     */
    private void createChatRoom(String matchId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue().get("match:" + matchId);
        if (userIds == null) return;

        Map<String, Object> request = Map.of("userIds", userIds);

        chatServiceClient.post()
                .uri("/room") // 채팅 서버의 엔드포인트
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block(); // 동기 처리 (비동기 처리하려면 Mono<Void> 반환)
    }

    /**
     * 매칭 완료 후 관련 데이터 정리
     */
    private void cleanupMatch(String matchId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue().get("match:" + matchId);
        if (userIds == null) return;

        for (String userId : userIds) {
            redisTemplate.delete("user:" + userId);
        }
        redisTemplate.delete("match:" + matchId);
    }

    /**
     * WebSocket을 통한 단일 유저에게 매칭 상태 전송
     */
    private void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/matching/" + userId,
                new MatchResponseDto(type, message, null)
        );
    }

    /**
     * 매칭 성사 알림 전송
     */
    private void notifyMatch(String user1Id, String user2Id, String matchId) {
        Map<String, Object> data = Map.of("matchId", matchId);

        messagingTemplate.convertAndSend(
                "/topic/matching/" + user1Id,
                new MatchResponseDto("MATCH_SUCCESS", "매칭이 성사되었습니다.", data)
        );
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user2Id,
                new MatchResponseDto("MATCH_SUCCESS", "매칭이 성사되었습니다.", data)
        );
    }
}
