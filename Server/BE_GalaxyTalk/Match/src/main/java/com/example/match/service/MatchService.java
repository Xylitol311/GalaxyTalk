package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchApproveRequestDto;
import com.example.match.dto.UserResponseDto;
import com.example.match.dto.UserStatusDto;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class MatchService {
    private final RedisService redisService;
    private final WebSocketService webSocketService;
    private final ExternalApiService externalApiService;
    private final MatchingQueueManager queueManager;
    private final MatchProcessor matchProcessor;

    /**
     * 매칭 시작 처리
     * 1. 유저 상태 Redis에 저장
     * 2. 선호하는 MBTI 큐에 추가
     * 3. WebSocket으로 대기 시작 알림
     */
    public void startMatching(UserMatchStatus user) {
        log.info("Starting Matching...");

        // 이미 매칭 중인 유저인지 확인
        UserMatchStatus userMatchStatus = redisService.getUserStatus(user.getUserId());
        if (userMatchStatus != null) {
            throw new BusinessException(ErrorCode.MATCH_ALREADY_IN_PROGRESS);
        }

        // 회원 정보 요청 및 MBTI 추출
//        UserResponseDto.UserSendDTO userResponse = externalApiService.getUserInfo(user.getUserId());
        Map<String, Object> userResponse = externalApiService.getUserInfo(user.getUserId());
//        String userMbti = (userResponse != null) ? userResponse.getMbti() : null;
        String userMbti = (userResponse != null) ? (String) userResponse.get("mbti") : null;

        if (userMbti == null) {
            log.warn("유저 {}의 MBTI 정보를 가져올 수 없습니다.", user.getUserId());
            throw new BusinessException(ErrorCode.USER_INFO_NOT_FOUND,
                    "유저 " + user.getUserId() + "의 MBTI 정보를 가져올 수 없습니다.");
        }

        user.setMbti(userMbti);
        user.setEnergy((Integer) userResponse.get("energy"));
        user.setStatus(MatchStatus.WAITING);
        user.setAccepted(false);
        user.setStartTime(Instant.now().toEpochMilli());

        // Redis에 유저 상태 저장
        redisService.saveUserStatus(user);

        // 매칭 큐에 추가
        queueManager.addToQueue(user);

        // ZSET에 유저 추가
        redisService.addUserToWaitingQueue(user);

        // 세션 서버에 매칭 상태 변경 요청
        externalApiService.setUserStatus(user.getUserId(), "matching");

        // 대기 상태 알림
        webSocketService.notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");
        // 새로운 유저 입장 알림
        webSocketService.broadcastNewUser(user);
    }


    /**
     * 매칭 취소 처리
     */
    public void cancelMatching(String userId) {
        // 매칭 중인 유저인지 확인
        UserMatchStatus userMatchStatus = redisService.getUserStatus(userId);
        if (userMatchStatus == null) {
            throw new BusinessException(ErrorCode.USER_NOT_MATCHED_STATUS);
        }

        // Redis에서 유저 정보 삭제
        redisService.deleteUserStatus(userId);

        // ZSET에서 매칭 대기 유저 제거
        redisService.removeUserFromWaitingQueue(userId);

        // 세션 서버 상태 변경
        externalApiService.setUserStatus(userId, "idle");

        // 유저 퇴장 알림
        webSocketService.broadcastUserExit(userId);
    }

    /**
     * 매칭 시작 시간 조회
     */
    public Long getMatchingStartTime(String userId) {
        UserMatchStatus user = redisService.getUserStatus(userId);
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
                .toList();

        // 모든 비동기 처리 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 특정 MBTI 큐의 매칭 처리를 비동기로 실행
     */
    @Async
    public CompletableFuture<Void> processMbtiQueue(MBTI mbti) {
//        List<UserMatchStatus> batch = queueManager.getBatchFromQueue(mbti);
        List<UserMatchStatus> batch = queueManager.getBatchFromQueue();
        if (batch.isEmpty()) {
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
        log.info("유저 목록 기반 매칭 처리 시작");
        log.info("이거봐라 몇명인지" + users.size());
        // 매칭 가능한 유저만 필터링
        List<UserMatchStatus> availableUsers = filterAvailableUsers(users);
        if (availableUsers.size() < 2) {
            log.info("필터링 돼서 두명보다 적음");
            return;
        }

        // 모든 가능한 페어의 유사도 계산 및 정렬
        List<MatchPair> sortedPairs = calculateAllPairsSimilarity(availableUsers);

        // 매칭 쌍 생성 및 처리
        processMatchPairs(sortedPairs);
    }

    /**
     * 매칭 가능한 유저 필터링
     * - Lazy Deletion 체크: Redis에서 user를 다시 조회하여 null이 아닌지, WAITING 상태인지 확인
     */
    private List<UserMatchStatus> filterAvailableUsers(List<UserMatchStatus> users) {
        return users.stream()
                .filter(user -> {
                    UserMatchStatus currentStatus = redisService.getUserStatus(user.getUserId());
                    return currentStatus != null && currentStatus.getStatus() == MatchStatus.WAITING;
                })
                .collect(Collectors.toList());
    }

    /**
     * 모든 가능한 페어의 유사도 계산
     */
    private List<MatchPair> calculateAllPairsSimilarity(List<UserMatchStatus> users) {
        log.info("주어진 페어쌍들의 유사도 검사 로직 시작");
        List<MatchPair> pairs = new ArrayList<>();

        for (int i = 0; i < users.size() - 1; i++) {
            for (int j = i + 1; j < users.size(); j++) {
                UserMatchStatus user1 = users.get(i);
                UserMatchStatus user2 = users.get(j);

                // 자기 자신과 매칭 방지
                if (user1.getUserId().equals(user2.getUserId()))
                    continue;

                double similarity = externalApiService.calculateSimilarity(user1, user2);
                pairs.add(new MatchPair(user1, user2, similarity));
            }
        }

        pairs.sort((p1, p2) -> Double.compare(p2.similarity, p1.similarity));
        return pairs;
    }

    /**
     * 매칭 쌍 처리
     */
    private void processMatchPairs(List<MatchPair> sortedPairs) {
        log.info("processMatchPairs 시작");
        // 매칭된 유저를 담는 Set
        Set<String> matchedUsers = new HashSet<>();

        for (MatchPair pair : sortedPairs) {
            // 이미 매칭이 돼서 matchedUsers에 저장된 유저면 패스
            if (matchedUsers.contains(pair.user1.getUserId()) ||
                    matchedUsers.contains(pair.user2.getUserId())) {
                continue;
            }

            // 매칭을 시도할 두 유저의 정보를 레디스에서 가져옴
            UserMatchStatus currentUser1 = redisService.getUserStatus(pair.user1.getUserId());
            UserMatchStatus currentUser2 = redisService.getUserStatus(pair.user2.getUserId());

            // 유효한 사용자(매칭 취소, 완료되지 않음)면서 매칭 대기 중인 유저인 경우만 매칭 진행
            if (currentUser1 != null && currentUser2 != null &&
                    currentUser1.getStatus() == MatchStatus.WAITING &&
                    currentUser2.getStatus() == MatchStatus.WAITING) {

                matchProcessor.createMatch(pair.user1, pair.user2, pair.similarity);
                matchedUsers.add(pair.user1.getUserId());
                matchedUsers.add(pair.user2.getUserId());
            }
        }
    }

    /**
     * 매칭 수락/거절 응답 처리
     * 유저의 응답에 따라 수락 또는 거절 프로세스 실행
     */
    public void processMatchApproval(String userId, MatchApproveRequestDto response) {
        UserMatchStatus user = redisService.getUserStatus(userId);

        if (user == null) {
            // 유저 정보를 찾을 수 없으면 예외
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "userId: " + userId);
        }

        if (!response.getMatchId().equals(user.getMatchId())) {
            // 매칭 ID가 일치하지 않으면 잘못된 요청
            throw new IllegalArgumentException("잘못된 매칭 ID입니다.");
        }

        // MatchProcessor를 통해 매칭 승인/거절 처리
        matchProcessor.processMatchResponse(user, response);
    }


    /**
     * 매칭 대기 중인 유저 목록 조회
     *
     */
    public List<UserStatusDto> getWaitingUsers() {
        List<String> randomUserIds = redisService.getRandomWaitingUsers(20);
        List<UserStatusDto> waitingUsers = new ArrayList<>();

        for (String userId : randomUserIds) {
            UserMatchStatus userStatus = redisService.getUserStatus(userId);
            if (userStatus != null) {
                waitingUsers.add(new UserStatusDto(
                        userStatus.getUserId(),
                        userStatus.getConcern(),
                        userStatus.getMbti(),
                        userStatus.getStatus(),
                        userStatus.getStartTime()
                ));
            }
        }
        return waitingUsers;
    }

    @Getter
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
