package com.example.match.service;

import com.example.match.domain.MatchResultStatus;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchApproveRequestDto;
import com.example.match.dto.MatchResponseDto;
import com.example.match.dto.UserStatusDto;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class MatchService {
    private final RedisService redisService;
    private final WebSocketService webSocketService;
    private final ExternalApiService externalApiService;
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
     * 1. 유저 상태를 Redis에 저장하고
     * 2. 대기 큐에 등록(매칭 시작 시간 기준)
     * 3. WebSocket으로 알림 전송
     */
    public void startMatching(UserMatchStatus user) {
        log.info("매칭 시작 요청: userId={}", user.getUserId());

        // 이미 매칭 중인 유저인지 확인
        UserMatchStatus existing = redisService.getUserStatus(user.getUserId());
        if (existing != null) {
            // 매칭 성사된 유저인 경우
            if (existing.getMatchId() != null) {
                matchProcessor.processMatchingCancle(existing.getMatchId(), user.getUserId());
            }
            // 기존 정보 삭제
            redisService.deleteUserStatus(user.getUserId());
            redisService.removeUserFromWaitingQueue(user.getUserId());
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

        // Redis 대기 큐(Waiting Pool)에 유저 등록
        redisService.addUserToWaitingQueue(user);

        // WebSocket 알림 전송 (매칭 대기 시작)
        externalApiService.setUserStatus(user.getUserId(), "matching");
        webSocketService.notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");

        // broadCasting
        webSocketService.broadcastNewUser(user);
    }


    /**
     * 매칭 취소 처리
     */
    public void cancelMatching(String userId) {
        // 매칭 중인 유저인지 확인
        UserMatchStatus userMatchStatus = redisService.getUserStatus(userId);
        if (userMatchStatus == null) {
            throw new BusinessException(ErrorCode.USER_NOT_MATCHING);
        }

        // 매칭이 성사된 유저인 경우 매칭 취소 및 상대방도 취소 처리 필요
        if (userMatchStatus.getMatchId() != null) {
            matchProcessor.processMatchingCancle(userMatchStatus.getMatchId(), userId);
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
     * 매칭 대기 중인 유저 목록 조회
     */
    public List<UserStatusDto> getWaitingUsers() {
        Set<String> waitingUserIds = redisService.getRandomWaitingUsers(20);
        List<UserStatusDto> list = new ArrayList<>();
        for (String id : waitingUserIds) {
            UserMatchStatus user = redisService.getUserStatus(id);
            if (user != null) {
                list.add(new UserStatusDto(
                        user.getUserId(),
                        user.getConcern(),
                        user.getMbti(),
                        user.getStatus(),
                        user.getStartTime()
                ));
            }
        }
        return list;
    }

    /**
     * 매칭 시작 시간 조회
     */
    public Long getMatchingStartTime(String userId) {
        UserMatchStatus user = redisService.getUserStatus(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        }
        return user.getStartTime();
    }

    /**
     * 매칭 수락/거절 응답 처리
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
     * 통합 대기 큐(Redis Waiting Pool)에 있는 WAITING 상태의 모든 유저를 대상으로 후보 쌍을 생성
     * 1. strict 조건: 서로의 선호하는 MBTI와 자신의 MBTI가 일치하는 경우
     * → composite score = 0.8 × 고민 유사도 + 0.2 × 1
     * 2. relaxed 조건: strict 조건 미충족 시, MBTI 유사도 매트릭스를 기반으로 MBTI 유사도를 계산
     * → composite score = 0.8 × 고민 유사도 + 0.2 × (계산된 MBTI 유사도)
     * strict 후보가 하나라도 있으면 우선 strict 후보로 매칭을 시도하고, 없으면 relaxed 후보로 진행
     */
    @Scheduled(fixedDelay = 5000)
    public void processMatchingQueue() {
        log.info("매칭 대기 큐 처리 시작");

        // Redis에서 모든 대기 유저 ID 조회
        Set<String> waitingUserIds = redisService.getAllWaitingUsers();
        if (waitingUserIds.isEmpty()) {
            log.info("대기 중인 유저가 없습니다.");
            return;
        }

        // WAITING 상태인 유저의 상태 객체들을 가져옴
        List<UserMatchStatus> waitingUsers = new ArrayList<>();
        for (String userId : waitingUserIds) {
            UserMatchStatus user = redisService.getUserStatus(userId);
            if (user != null && user.getStatus() == MatchStatus.WAITING) {

                waitingUsers.add(user);
            }
        }

        // 5분 이상 매칭 중인 유저 제거
        for (UserMatchStatus user : waitingUsers) {
            if (checkWaitingTimeout(user)){
                waitingUsers.remove(user);
            }
        }

        log.info("대기 중인 유저 수: {}", waitingUsers.size());

        List<MatchPair> strictPairs = new ArrayList<>();
        List<MatchPair> relaxedPairs = new ArrayList<>();

        // 모든 후보 쌍에 대해 검사
        for (int i = 0; i < waitingUsers.size() - 1; i++) {
            for (int j = i + 1; j < waitingUsers.size(); j++) {
                UserMatchStatus u1 = waitingUsers.get(i);
                UserMatchStatus u2 = waitingUsers.get(j);

                // 같은 사람인지 확인
                if (u1.getUserId().equals(u2.getUserId()))
                    continue;

                // 거절했던 상대라면 매칭 후보에서 제외
                if (redisService.hasRejected(u1.getUserId(), u2.getUserId()) ||
                redisService.hasRejected(u2.getUserId(), u1.getUserId()))
                    continue;


                // 고민 유사도 계산 (기존 외부 API 활용)
                double concernSim = externalApiService.calculateSimilarity(u1, u2);
                if (isStrictCompatible(u1, u2)) {
                    // strict 조건: 두 유저의 선호와 자신의 MBTI가 완벽히 일치하면 MBTI 점수를 1로 간주
                    double compositeScore = 0.8 * concernSim + 0.2 * 1.0;
                    strictPairs.add(new MatchPair(u1, u2, compositeScore));
                } else {
                    // relaxed 조건: MBTI 유사도 계산 (각 자리 일치 비율)
                    double mbtiSim = calculateMBTISimilarity(u1.getMbti(), u2.getMbti());
                    double compositeScore = 0.8 * concernSim + 0.2 * mbtiSim;
                    relaxedPairs.add(new MatchPair(u1, u2, compositeScore));
                }
            }
        }

        // strict 후보들을 우선 처리
        strictPairs.sort((p1, p2) -> Double.compare(p2.similarity, p1.similarity));
        Set<String> matchedUserIds = new HashSet<>();
        for (MatchPair pair : strictPairs) {
            if (matchedUserIds.contains(pair.user1.getUserId()) || matchedUserIds.contains(pair.user2.getUserId())) {
                continue;
            }
            // 원자적 상태 전환 시도: WAITING -> IN_PROGRESS
            boolean trans1 = redisService.atomicTransitionToInProgress(pair.user1.getUserId());
            boolean trans2 = redisService.atomicTransitionToInProgress(pair.user2.getUserId());
            if (trans1 && trans2) {
                // 매칭 처리: 매칭 생성 및 상태 업데이트
                matchProcessor.createMatch(pair.user1, pair.user2, pair.similarity);
                matchedUserIds.add(pair.user1.getUserId());
                matchedUserIds.add(pair.user2.getUserId());
            }
        }

        // strict 후보 처리 후, 남은 유저들에 대해 relaxed 후보들을 처리
        relaxedPairs.sort((p1, p2) -> Double.compare(p2.similarity, p1.similarity));
        for (MatchPair pair : relaxedPairs) {
            if (matchedUserIds.contains(pair.user1.getUserId()) || matchedUserIds.contains(pair.user2.getUserId())) {
                continue;
            }
            // 원자적 상태 전환 시도: WAITING -> IN_PROGRESS
            boolean trans1 = redisService.atomicTransitionToInProgress(pair.user1.getUserId());
            boolean trans2 = redisService.atomicTransitionToInProgress(pair.user2.getUserId());
            if (trans1 && trans2) {
                // 매칭 처리: 매칭 생성 및 상태 업데이트
                matchProcessor.createMatch(pair.user1, pair.user2, pair.similarity);
                matchedUserIds.add(pair.user1.getUserId());
                matchedUserIds.add(pair.user2.getUserId());
            }
        }
    }

    /**
     * strict 조건: A의 선호 MBTI가 B의 자신의 MBTI와 일치하고, B의 선호 MBTI가 A의 자신의 MBTI와 일치하는지 확인
     */
    private boolean isStrictCompatible(UserMatchStatus u1, UserMatchStatus u2) {
        if (u1.getPreferredMbti() == null || u2.getPreferredMbti() == null ||
                u1.getMbti() == null || u2.getMbti() == null) {
            return false;
        }
        return u1.getPreferredMbti().equalsIgnoreCase(u2.getMbti()) &&
                u2.getPreferredMbti().equalsIgnoreCase(u1.getMbti());
    }

    /**
     * MBTI 유사도 계산
     * 간단히 각 자리별 일치하는 글자의 비율(0~1)을 반환.
     */
    private double calculateMBTISimilarity(String mbti1, String mbti2) {
        if (mbti1 == null || mbti2 == null || mbti1.length() != 4 || mbti2.length() != 4) {
            return 0.0;
        }
        int matchCount = 0;
        mbti1 = mbti1.toUpperCase();
        mbti2 = mbti2.toUpperCase();
        for (int i = 0; i < 4; i++) {
            if (mbti1.charAt(i) == mbti2.charAt(i)) {
                matchCount++;
            }
        }
        return matchCount / 4.0;
    }

    public boolean checkWaitingTimeout(UserMatchStatus user) {
        log.info("대기 시간 초과 유저 검사");
        long now = Instant.now().toEpochMilli();
        String userId = user.getUserId();
        if (user != null && user.getStatus() == MatchStatus.WAITING) {
            // 시작 시각으로부터 5분(300,000ms) 경과 여부 확인
            if (now - user.getStartTime() > 300_000) {
                log.info("유저 {} 대기 시간 초과로 매칭 취소", userId);
                // Redis에서 상태 삭제 및 대기 큐에서 제거
                redisService.deleteUserStatus(userId);
                redisService.removeUserFromWaitingQueue(userId);
                // 외부 API를 통해 세션 상태 변경 (예: idle)
                externalApiService.setUserStatus(userId, "idle");
                // WebSocket 알림 전송
                webSocketService.notifyUser(userId, "CANCEL_WAITING", "매칭 대기 취소");
                // 매칭 취소 브로드캐스팅
                webSocketService.broadcastUserExit(userId);

                return true;
            }
        }
        return false;
    }

    /**
     * 매칭 대기 중인 유저 중, 시작 후 5분이 지난 유저를 자동 취소
     * 이 메서드는 1분마다 실행
     */
    @Scheduled(fixedDelay = 60000)
    public void checkWaitingTimeout() {
        log.info("대기 시간 초과 유저 검사 시작");
        Set<String> waitingUserIds = redisService.getAllWaitingUsers();
        long now = Instant.now().toEpochMilli();
        for (String userId : waitingUserIds) {
            UserMatchStatus user = redisService.getUserStatus(userId);
            if (user != null && user.getStatus() == MatchStatus.WAITING) {
                // 시작 시각으로부터 5분(300,000ms) 경과 여부 확인
                if (now - user.getStartTime() > 300_000) {
                    log.info("유저 {} 대기 시간 초과로 매칭 취소", userId);
                    // Redis에서 상태 삭제 및 대기 큐에서 제거
                    redisService.deleteUserStatus(userId);
                    redisService.removeUserFromWaitingQueue(userId);
                    // 외부 API를 통해 세션 상태 변경 (예: idle)
                    externalApiService.setUserStatus(userId, "idle");
                    // WebSocket 알림 전송
                    webSocketService.notifyUser(userId, "CANCEL_WAITING", "매칭 대기 취소");
                    // 매칭 취소 브로드캐스팅
                    webSocketService.broadcastUserExit(userId);
                }
            }
        }
    }

    /**
     * 매칭 정보 조회
     * - 유저가 매칭된 매칭 정보와 매칭 상대방에 대한 정보 반환
     *
     */
    public MatchResponseDto getMatchInfo(String userId, String matchId) {
        MatchResponseDto matchResponseDto = new MatchResponseDto();
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        // user가 거절한 상대방 id 추출
        String otherUserId = matchResultStatus.getUserIds().stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);



        return matchResponseDto;
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
