package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import com.example.match.dto.UserResponseDto;
import com.example.match.dto.UserStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MatchService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MatchingQueueManager queueManager;
    private final WebClient aiServiceClient;
    private final WebClient chatServiceClient;
    private final WebClient authServiceClient;

    // 매칭 관련 상수
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final String USER_KEY_PREFIX = "user:";

    /**
     * 매칭 시작 처리
     * 1. 유저 상태 Redis에 저장
     * 2. 선호하는 MBTI 큐에 추가
     * 3. WebSocket으로 대기 시작 알림
     */
    public void startMatching(UserMatchStatus user) {
        String userKey = USER_KEY_PREFIX + user.getUserId();

        // 회원 정보 요청 및 MBTI 추출
        UserResponseDto userResponse = authServiceClient.get()
                .uri("/api/oauth?userId=" + user.getUserId()) // 필요 시 userId 추가
                .retrieve()
                .bodyToMono(UserResponseDto.class)
                .block(); // 동기 처리 (비동기 처리하려면 Mono<UserResponse> 반환)

        String userMbti = (userResponse != null) ? userResponse.getMbti() : null;

        if (userMbti == null) {
            log.warn("유저 {}의 MBTI 정보를 가져올 수 없습니다.", user.getUserId());
            return;
        }

        user.setMbti(userMbti);
        user.setStatus(MatchStatus.WAITING);
        user.setStartTime(System.currentTimeMillis());

        // Redis에 유저 상태 저장
        redisTemplate.opsForValue().set(userKey, user);

        // 매칭 큐에 추가
        queueManager.addToQueue(user);

        // 대기 상태 알림
        notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");

        // 새로운 유저 입장 알림
        broadcastNewUser(user);
    }

    /**
     * 매칭 취소 처리
     */
    public void cancelMatching(String userId) {
        String userKey = USER_KEY_PREFIX + userId;

        // Redis에서 유저 정보 삭제
        redisTemplate.delete(userKey);

        // 유저 퇴장 알림
        broadcastUserExit(userId);
    }

    /**
     * 매칭 시작 시간 조회
     */
    public Long getMatchingStartTime(String userId) {
        String userKey = USER_KEY_PREFIX + userId;
        UserMatchStatus user = (UserMatchStatus) redisTemplate.opsForValue().get(userKey);
        return user != null ? user.getStartTime() : null;
    }

    /**
     * 각 MBTI 큐별로 매칭 프로세스 실행
     * 5초마다 모든 큐에 대해 비동기로 처리
     */
    @Scheduled(fixedDelay = 5000)
    public void processAllQueues() {
        List<CompletableFuture<Void>> futures = Arrays.stream(MBTI.values())
                .map(this::processMbtiQueue)
                .collect(Collectors.toList());

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
            if (batch.size() == 1) {
                queueManager.moveToRelatedQueue(batch.get(0));
            }
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
        // 매칭 가능한 유저만 필터링
        List<UserMatchStatus> availableUsers = filterAvailableUsers(users);
        if (availableUsers.size() < 2) return;

        // 모든 가능한 페어의 유사도 계산 및 정렬
        List<MatchPair> sortedPairs = calculateAllPairsSimilarity(availableUsers);

        // 매칭 쌍 생성 및 처리
        processMatchPairs(sortedPairs);
    }

    /**
     * 매칭 가능한 유저 필터링
     */
    private List<UserMatchStatus> filterAvailableUsers(List<UserMatchStatus> users) {
        return users.stream()
                .filter(user -> {
                    String userKey = USER_KEY_PREFIX + user.getUserId();
                    UserMatchStatus currentStatus =
                            (UserMatchStatus) redisTemplate.opsForValue().get(userKey);
                    return currentStatus != null &&
                            currentStatus.getStatus() == MatchStatus.WAITING;
                })
                .collect(Collectors.toList());
    }

    /**
     * 모든 가능한 페어의 유사도 계산
     */
    private List<MatchPair> calculateAllPairsSimilarity(List<UserMatchStatus> users) {
        List<MatchPair> pairs = new ArrayList<>();

        for (int i = 0; i < users.size() - 1; i++) {
            for (int j = i + 1; j < users.size(); j++) {
                UserMatchStatus user1 = users.get(i);
                UserMatchStatus user2 = users.get(j);

                double similarity = calculateSimilarity(user1, user2);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    pairs.add(new MatchPair(user1, user2, similarity));
                }
            }
        }

        // 유사도 높은 순으로 정렬
        pairs.sort((p1, p2) -> Double.compare(p2.similarity, p1.similarity));
        return pairs;
    }

    /**
     * 매칭 쌍 처리
     */
    private void processMatchPairs(List<MatchPair> sortedPairs) {
        Set<String> matchedUsers = new HashSet<>();

        for (MatchPair pair : sortedPairs) {
            if (matchedUsers.contains(pair.user1.getUserId()) ||
                    matchedUsers.contains(pair.user2.getUserId())) {
                continue;
            }

            // 매칭된 유저들의 현재 상태 확인
            UserMatchStatus currentUser1 = (UserMatchStatus) redisTemplate.opsForValue()
                    .get(USER_KEY_PREFIX + pair.user1.getUserId());
            UserMatchStatus currentUser2 = (UserMatchStatus) redisTemplate.opsForValue()
                    .get(USER_KEY_PREFIX + pair.user2.getUserId());

            // 둘 다 대기 중인 경우만 매칭 진행
            if (currentUser1 != null && currentUser2 != null &&
                    currentUser1.getStatus() == MatchStatus.WAITING &&
                    currentUser2.getStatus() == MatchStatus.WAITING) {

                createMatch(pair.user1, pair.user2);
                matchedUsers.add(pair.user1.getUserId());
                matchedUsers.add(pair.user2.getUserId());
            } else {
                // 유저가 없거나 대기 상태가 아닌 경우 해당 큐에서만 제거
                if (currentUser1 == null) {
                    queueManager.removeFromQueueIfInvalid(
                            MBTI.valueOf(pair.user1.getMbti()), pair.user1);
                }
                if (currentUser2 == null) {
                    queueManager.removeFromQueueIfInvalid(
                            MBTI.valueOf(pair.user2.getMbti()), pair.user2);
                }
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

    /**
     * 새로운 유저 입장 알림
     */
    private void broadcastNewUser(UserMatchStatus user) {
        UserStatusDto statusDto = new UserStatusDto(
                user.getUserId(),
                user.getConcern(),
                user.getMbti()
        );
        messagingTemplate.convertAndSend(
                "/topic/matching/users/new",
                statusDto
        );
    }

    /**
     * 유저 퇴장 알림
     */
    private void broadcastUserExit(String userId) {
        messagingTemplate.convertAndSend(
                "/topic/matching/users/exit",
                userId
        );
    }

    /**
     * 매칭 페어 클래스
     */
    private static class MatchPair {
        private final UserMatchStatus user1;
        private final UserMatchStatus user2;
        private final double similarity;

        public MatchPair(UserMatchStatus user1, UserMatchStatus user2, double similarity) {
            this.user1 = user1;
            this.user2 = user2;
            this.similarity = similarity;
        }
    }
}
