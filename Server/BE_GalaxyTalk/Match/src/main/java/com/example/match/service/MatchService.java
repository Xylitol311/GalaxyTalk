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
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
     * @Async 메서드를 같은 클래스 내부에서 호출할 경우 프록시를 거치지 않아 비동기가 작동 불가.
     * AopContext.currentProxy()를 사용하여 자기자신의 프록시를 통해 호출
     */
    @Autowired
    @Lazy
    private MatchService self;


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

        // 외부 Auth 서버(WebClient 사용)를 통해 회원 정보 조회 및 MBTI, 에너지 등 추출
        Map<String, Object> userResponse = externalApiService.getUserInfo(user.getUserId());
        String userMbti = (userResponse != null) ? (String) userResponse.get("mbti") : null;
        if (userMbti == null) {
            log.warn("유저 {}의 MBTI 정보를 가져올 수 없습니다.", user.getUserId());
            throw new BusinessException(ErrorCode.USER_INFO_NOT_FOUND,
                    "유저 " + user.getUserId() + "의 MBTI 정보를 가져올 수 없습니다.");
        }

        // 유저 객체에 기본 정보 설정
        user.setMbti(userMbti);
        user.setEnergy((Integer) userResponse.get("energy"));
        // 초기 상태는 WAITING (큐 등록 후 매칭 작업 시 IN_PROGRESS로 전환)
        user.setStatus(MatchStatus.WAITING);
        user.setAccepted(false);
        user.setStartTime(Instant.now().toEpochMilli());

        // Redis에 유저 상태 저장
        redisService.saveUserStatus(user);

        // 해당 사용자를 자신의 MBTI 큐 및 (선호하는 MBTI가 있으면) 해당 큐에도 추가
        queueManager.addToQueue(user);

        // Redis Sorted Set에 대기 유저 등록 (매칭 대기 시작 시간 기록)
        redisService.addUserToWaitingQueue(user);

        // 세션 서버에 매칭 상태 변경 요청 (WebClient 사용)
        externalApiService.setUserStatus(user.getUserId(), "matching");

        // WebSocket으로 대기 상태 및 새로운 유저 입장 알림 전송
        webSocketService.notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");
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
     * 각 MBTI 큐별로 매칭 프로세스 실행
     * 5초마다 모든 큐에 대해 비동기로 처리
     * AopContext.currentProxy()를 통해 자기자신의 프록시를 호출하여 @Async가 적용되도록 함.
     */
    @Scheduled(fixedDelay = 5000)
    public void processAllQueues() {
        List<CompletableFuture<Void>> futures = Arrays.stream(MBTI.values())
                .map(mbti -> self.processMbtiQueue(mbti))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 특정 MBTI 큐에서 배치 추출 및 매칭 처리
     * WAITING 상태인 후보 유저들을 원자적으로 IN_PROGRESS로 전환한 후,
     * 전환에 성공한 유저들을 배치에 포함.
     */

    @Async
    public CompletableFuture<Void> processMbtiQueue(MBTI mbti) {
        // getBatchFromQueue를 호출하여 후보 리스트를 구성합니다.
        List<UserMatchStatus> batch = queueManager.getIntegratedBatchFromQueue(mbti);
        List<UserMatchStatus> finalBatch = new ArrayList<>();
        for (UserMatchStatus user : batch) {
            // 원자적 상태 전환: WAITING → IN_PROGRESS
            boolean transitioned = redisService.atomicTransitionToInProgress(user.getUserId());
            if (transitioned) {
                UserMatchStatus updated = redisService.getUserStatus(user.getUserId());
                if (updated != null) {
                    finalBatch.add(updated);
                }
            }
        }
        if (finalBatch.size() < 2) {
            log.info("배치 후보가 {}명으로 부족하여 재큐 처리합니다.", finalBatch.size());
            for (UserMatchStatus user : finalBatch) {
                user.setStatus(MatchStatus.WAITING);
                redisService.saveUserStatus(user);
                redisService.addUserToWaitingQueue(user);
                queueManager.addToQueue(user);
            }
            return CompletableFuture.completedFuture(null);
        }
        processMatching(finalBatch);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 배치 단위 매칭 처리
     * 0. 만약 배치에 2명 미만인 유저가 남으면, 해당 유저들은 재큐되어 이후 처리될 수 있도록 처리
     * 1. 필터: WAITING 상태 및 거절 기록을 고려하여 매칭 가능한 유저만 선별
     * 2. 모든 가능한 페어에 대해 유사도 계산 후 정렬
     * 3. 유사도가 높은 순으로 매칭 진행
     * 4. filterAvailableUsers는 IN_PROGRESS 상태의 유저들만 선택하도록 수정.
     * 5. 매칭 후, 매칭되지 못한 유저는 즉시 WAITING 상태로 전환하여 재등록하는 로직 추가.
     */
    private void processMatching(List<UserMatchStatus> users) {
        log.info("매칭 작업 진행, 배치 사이즈: {}", users.size());

        // IN_PROGRESS 상태의 유저들만 대상으로 함.
        List<UserMatchStatus> availableUsers = users.stream()
                .filter(user -> {
                    UserMatchStatus current = redisService.getUserStatus(user.getUserId());
                    return current != null && current.getStatus() == MatchStatus.IN_PROGRESS;
                })
                .collect(Collectors.toList());

        // 매칭 가능한 인원이 1명이면
        if (availableUsers.size() < 2) {
            log.info("매칭 가능한 유저가 2명 미만입니다.");
            // 재큐: 배치에 2명 미만이면, 상태를 다시 WAITING으로 전환하고 큐에 추가
            for (UserMatchStatus user : availableUsers) {
                user.setStatus(MatchStatus.WAITING);
                redisService.saveUserStatus(user);
                // WAITING 큐에 다시 등록
                redisService.addUserToWaitingQueue(user);
                queueManager.addToQueue(user);
            }
            return;
        }
        List<MatchPair> sortedPairs = calculateAllPairsSimilarity(availableUsers);
        // processMatchPairs를 수행하고 매칭된 유저 ID 집합을 반환받음.
        Set<String> matchedUserIds = processMatchPairs(sortedPairs);

        // 매칭되지 않은 유저들은 이후 스케줄러(재큐 로직)에서 WAITING으로 전환됨.
    }

    /**
     * 모든 가능한 페어의 유사도 계산
     * 단, 두 유저 중 하나가 상대방을 이전에 거절한 경우 해당 페어는 매칭 대상에서 제외
     */
    private List<MatchPair> calculateAllPairsSimilarity(List<UserMatchStatus> users) {
        log.info("페어 유사도 계산 시작");
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
     * 매칭 쌍을 순차적으로 처리하여 매칭을 생성
     * - 매칭에 성공하면, 해당 유저들의 상태를 MATCHED로 업데이트하여 이후 배치에서 후보로 선택되지 않게 설정.
     * - 반환값은 매칭에 성공한 유저 ID의 집합으로, 중복 매칭 방지에 사용.
     */
    private Set<String> processMatchPairs(List<MatchPair> sortedPairs) {
        log.info("processMatchPairs 시작");
        // 매칭된 유저를 담는 Set
        Set<String> matchedUsers = new HashSet<>();

        for (MatchPair pair : sortedPairs) {
            // 이미 매칭이 돼서 matchedUsers에 저장된 유저면 패스
            if (matchedUsers.contains(pair.user1.getUserId()) || matchedUsers.contains(pair.user2.getUserId())) {
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
        return matchedUsers;
    }

    /**
     * 매칭 수락/거절 응답 처리
     * 유저의 응답에 따라 수락 또는 거절 프로세스 실행
     */
    public void processMatchApproval(String userId, MatchApproveRequestDto response) {
        UserMatchStatus user = redisService.getUserStatus(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "userId: " + userId);
        }
        if (!response.getMatchId().equals(user.getMatchId())) {
            throw new IllegalArgumentException("잘못된 매칭 ID입니다.");
        }
        matchProcessor.processMatchResponse(user, response);
    }

    /**
     * 매칭 시작 시간 조회
     */
    public Long getMatchingStartTime(String userId) {
        UserMatchStatus user = redisService.getUserStatus(userId);
        return user != null ? user.getStartTime() : null;
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

    // 스케줄러: 5분 이상 WAITING 상태인 유저 자동 취소 처리
    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredWaitingUsers() {
        long expirationTime = Instant.now().toEpochMilli() - (5 * 60 * 1000); // 5분
        Set<String> expiredUsers = redisService.getWaitingUsersByScore(expirationTime);
        for (String userId : expiredUsers) {
            log.info("매칭 대기 5분 초과로 취소: {}", userId);
            cancelMatching(userId);
        }
    }

//    // 스케줄러: 1분 이상 IN_PROGRESS 상태인 유저를 다시 WAITING 상태로 전환 및 재큐 등록
//    @Scheduled(fixedDelay = 60000)
//    public void requeueInProgressUsers() {
//        long threshold = Instant.now().toEpochMilli() - (60 * 1000); // 1분
//        Set<String> stuckUsers = redisService.getInProgressUsersByScore(threshold);
//        for (String userId : stuckUsers) {
//            UserMatchStatus user = redisService.getUserStatus(userId);
//            if (user != null && user.getStatus() == MatchStatus.IN_PROGRESS) {
//                log.info("1분 이상 IN_PROGRESS 상태인 유저 재큐: {}", userId);
//                // 상태 변경 및 재등록
//                user.setStatus(MatchStatus.WAITING);
//                redisService.saveUserStatus(user);
//                redisService.removeUserFromInProgressQueue(userId);
//                redisService.addUserToWaitingQueue(user);
//                // 재큐: 해당 사용자가 속한 MBTI 큐(본인, 선호하는 MBTI)에도 추가
//                queueManager.addToQueue(user);
//            }
//        }
//    }

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
