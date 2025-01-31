package com.example.match.service;

import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

/**
 * 매칭 서비스 구현체
 * - 실시간 매칭 요청 처리
 * - 대기 사용자 주기적 처리
 * - Redis 기반 상태 관리
 * - WebSocket 기반 실시간 알림
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridMatchService implements MatchServiceInterface {
    // 외부 서비스 의존성
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebClient aiServiceClient;
    private final WebClient chatServiceClient;

    // 매칭 작업 큐
    private final BlockingQueue<MatchingTask> matchingQueue = new LinkedBlockingQueue<>();

    // 매칭 스레드 상태 관리
    private volatile boolean isRunning = false;
    private Thread matchingThread;

    // Redis 키 상수
    private static final String WAITING_USERS_KEY = "waiting_users";
    private static final String USER_KEY_PREFIX = "user:";
    private static final String MATCH_KEY_PREFIX = "match:";

    // 매칭 관련 상수
    private static final int CANDIDATE_POOL_SIZE = 50;
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final double MBTI_BONUS = 0.3;
    private static final long TIMEOUT_THRESHOLD = 300000; // 5분

    /**
     * 서비스 초기화
     * 매칭 처리 스레드 시작
     */
    @PostConstruct
    public void initialize() {
        startMatchingService();
    }

    /**
     * 서비스 종료
     * 매칭 처리 스레드 정리
     */
    @PreDestroy
    public void cleanup() {
        stopMatchingService();
    }

    /**
     * 매칭 서비스 시작
     * 지속적인 매칭 처리를 위한 스레드 실행
     */
    private void startMatchingService() {
        if (isRunning) return;

        isRunning = true;
        matchingThread = new Thread(() -> {
            while (isRunning) {
                try {
                    // 매칭 큐에서 태스크 가져오기
                    MatchingTask task = matchingQueue.poll(100, TimeUnit.MILLISECONDS);

                    if (task != null) {
                        // 새로운 매칭 요청 처리
                        processNewMatchingRequest(task);
                    } else {
                        // 대기 중인 사용자들 처리
                        processWaitingUsers();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("매칭 처리 중 오류 발생", e);
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        matchingThread.start();
        log.info("매칭 서비스가 시작되었습니다.");
    }

    /**
     * 새로운 매칭 시작 요청 처리
     */
    @Override
    public void startMatching(UserMatchStatus user) {
        try {
            // Redis에 사용자 상태 저장
            String userKey = USER_KEY_PREFIX + user.getUserId();
            user.setStatus(MatchStatus.WAITING);
            user.setStartTime(System.currentTimeMillis());
            redisTemplate.opsForValue().set(userKey, user);

            // 대기열에 추가
            redisTemplate.opsForZSet().add(
                    WAITING_USERS_KEY,
                    user.getUserId(),
                    -System.currentTimeMillis()
            );

            // 즉시 매칭 시도를 위한 태스크 추가
            matchingQueue.offer(new MatchingTask(user.getUserId()));

            // 대기 시작 알림
            notifyUser(user.getUserId(), "WAITING", "매칭 대기가 시작되었습니다.");

            log.info("사용자 {} 매칭 대기 시작", user.getUserId());
        } catch (Exception e) {
            log.error("매칭 시작 중 오류 발생: {}", user.getUserId(), e);
            throw new MatchingException("매칭 시작 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 새로운 매칭 요청 처리
     * 즉시 매칭 시도
     */
    private void processNewMatchingRequest(MatchingTask task) {
        UserMatchStatus user = getUserStatus(task.getUserId());
        if (user == null || user.getStatus() != MatchStatus.WAITING) {
            return;
        }

        // MBTI 선호도를 고려한 후보군 선택
        Set<String> candidates = selectCandidates(user);
        if (!candidates.isEmpty()) {
            processCandidates(user, candidates, null);
        }
    }

    /**
     * 대기 중인 사용자들 처리
     * 대기 시간이 긴 순서대로 처리
     */
    private void processWaitingUsers() {
        // 대기 시간이 가장 긴 사용자 조회
        Set<Object> rawSet = redisTemplate.opsForZSet()
                .range(WAITING_USERS_KEY, 0, 0);
        Set<String> oldestUsers = rawSet.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        if (oldestUsers == null || oldestUsers.isEmpty()) {
            return;
        }

        String oldestUserId = oldestUsers.iterator().next();
        UserMatchStatus oldestUser = getUserStatus(oldestUserId);

        if (oldestUser == null || oldestUser.getStatus() != MatchStatus.WAITING) {
            return;
        }

        // 대기 시간이 임계값을 초과한 경우
        long waitingTime = System.currentTimeMillis() - oldestUser.getStartTime();
        if (waitingTime > TIMEOUT_THRESHOLD) {
            handleTimeoutUser(oldestUser);
            return;
        }

        // 일반적인 매칭 처리
        Set<String> candidates = selectCandidates(oldestUser);
        if (!candidates.isEmpty()) {
            processCandidates(oldestUser, candidates, null);
        }
    }

    private Set<String> selectCandidates(UserMatchStatus baseUser) {
        // Redis의 ZSet에서 대기 시간 순으로 정렬된 사용자 목록 조회
        Set<String> rawCandidates = redisTemplate.opsForZSet()
                .rangeByScore(WAITING_USERS_KEY,
                        Double.NEGATIVE_INFINITY,
                        -System.currentTimeMillis())
                .stream()
                .map(Object::toString)  // Object를 String으로 변환
                .filter(userId -> !userId.equals(baseUser.getUserId()))
                .filter(userId -> {
                    UserMatchStatus candidate = getUserStatus(userId);
                    return isValidCandidate(baseUser, candidate);
                })
                .limit(CANDIDATE_POOL_SIZE)
                .collect(Collectors.toSet());

        return rawCandidates;
    }

    private boolean isValidCandidate(UserMatchStatus baseUser, UserMatchStatus candidate) {
        if (candidate == null || candidate.getStatus() != MatchStatus.WAITING) {
            return false;
        }

        // MBTI 선호도 확인
        boolean mbtiMatch = baseUser.getPreferredMbti() == null ||
                candidate.getMbti().equals(baseUser.getPreferredMbti());

        return mbtiMatch;
    }

    private void stopMatchingService() {
        if (!isRunning) return;

        isRunning = false;
        if (matchingThread != null) {
            matchingThread.interrupt();
            try {
                matchingThread.join(5000);  // 최대 5초 대기
            } catch (InterruptedException e) {
                log.error("매칭 서비스 종료 중 인터럽트 발생", e);
                Thread.currentThread().interrupt();
            }
        }
        log.info("매칭 서비스가 종료되었습니다.");
    }

    /**
     * 타임아웃된 사용자 처리
     */
    private void handleTimeoutUser(UserMatchStatus user) {
        // MBTI 선호도를 무시하고 더 넓은 범위에서 후보 검색
        Set<Object> rawSet = redisTemplate.opsForZSet()
                .range(WAITING_USERS_KEY, 0, -1);
        Set<String> allCandidates = rawSet.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        if (allCandidates == null || allCandidates.isEmpty()) {
            notifyTimeoutOptions(user);
            return;
        }

        // 매칭 임계값을 낮추어 처리
        processCandidates(user, allCandidates, SIMILARITY_THRESHOLD * 0.8);
    }

    /**
     * 매칭 후보군과 기준 사용자의 매칭 처리
     * @param baseUser 기준 사용자
     * @param candidates 후보군 목록
     * @param threshold 매칭 임계값 (기본값 사용 시 null)
     */
    private void processCandidates(UserMatchStatus baseUser, Set<String> candidates, Double threshold) {
        double matchThreshold = threshold != null ? threshold : SIMILARITY_THRESHOLD;

        // 후보들의 매칭 점수를 비동기로 계산
        List<CompletableFuture<MatchScore>> scoreFutures = candidates.stream()
                .map(candidateId -> getUserStatus(candidateId))
                .filter(candidate -> isValidCandidate(baseUser, candidate))
                .map(candidate -> calculateMatchScoreAsync(baseUser, candidate))
                .collect(Collectors.toList());

        // 모든 점수 계산이 완료될 때까지 대기 후 최적 매칭 선택
        CompletableFuture.allOf(scoreFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> scoreFutures.stream()
                        .map(CompletableFuture::join)
                        .max(Comparator.comparing(MatchScore::getScore)))
                .thenAccept(optimalMatch -> {
                    if (optimalMatch.isPresent() && optimalMatch.get().getScore() >= matchThreshold) {
                        createMatch(optimalMatch.get());
                    }
                })
                .exceptionally(e -> {
                    log.error("매칭 점수 계산 중 오류 발생", e);
                    return null;
                });
    }

    /**
     * 매칭 점수 비동기 계산
     * AI 서버와의 통신을 통한 유사도 계산 및 MBTI 보너스 적용
     */
    private CompletableFuture<MatchScore> calculateMatchScoreAsync(
            UserMatchStatus user1, UserMatchStatus user2) {
        return calculateSimilarity(user1.getConcern(), user2.getConcern())
                .thenApply(similarity -> {
                    // MBTI 선호도에 따른 보너스 점수 계산
                    double mbtiBonus = calculateMbtiBonus(user1, user2);
                    return new MatchScore(user1, user2, similarity + mbtiBonus);
                });
    }

    /**
     * AI 서버에 고민 내용 유사도 계산 요청
     * 에러 처리와 재시도 로직 포함
     */
    private CompletableFuture<Double> calculateSimilarity(String concern1, String concern2) {
        return aiServiceClient.post()
                .uri("/calculate-similarity")
                .bodyValue(Map.of(
                        "text1", concern1,
                        "text2", concern2
                ))
                .retrieve()
                .bodyToMono(Double.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                        .filter(e -> e instanceof WebClientResponseException))
                .toFuture()
                .exceptionally(e -> {
                    log.error("유사도 계산 중 오류 발생", e);
                    return 0.0;
                });
    }

    /**
     * MBTI 선호도에 따른 보너스 점수 계산
     */
    private double calculateMbtiBonus(UserMatchStatus user1, UserMatchStatus user2) {
        double bonus = 0.0;

        // 상호 MBTI 선호도 확인
        if (user1.getPreferredMbti().equals(user2.getMbti()) ||
                user2.getPreferredMbti().equals(user1.getMbti())) {
            bonus += MBTI_BONUS;
        }

        return bonus;
    }

    /**
     * 매칭 생성 및 처리
     * 매칭된 사용자들의 상태 업데이트 및 알림 전송
     */
    private void createMatch(MatchScore matchScore) {
        String matchId = UUID.randomUUID().toString();
        UserMatchStatus user1 = matchScore.getUser1();
        UserMatchStatus user2 = matchScore.getUser2();

        try {
            // 매칭 상태 업데이트
            updateMatchStatus(user1, user2, matchId);

            // 대기열에서 제거
            redisTemplate.opsForZSet().remove(
                    WAITING_USERS_KEY,
                    user1.getUserId(),
                    user2.getUserId()
            );

            // 매칭 성사 알림
            notifyMatchSuccess(user1.getUserId(), user2.getUserId(), matchId);

            log.info("매칭 성사: {} - {}", user1.getUserId(), user2.getUserId());
        } catch (Exception e) {
            log.error("매칭 생성 중 오류 발생", e);
            // 실패 시 상태 롤백
            rollbackMatchStatus(user1, user2);
        }
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

    private void rollbackMatchStatus(UserMatchStatus user1, UserMatchStatus user2) {
        try {
            // 원래 상태로 복구
            user1.setStatus(MatchStatus.WAITING);
            user2.setStatus(MatchStatus.WAITING);
            user1.setMatchId(null);
            user2.setMatchId(null);

            // Redis 상태 업데이트
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + user1.getUserId(), user1);
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + user2.getUserId(), user2);

            // 대기열에 재추가
            redisTemplate.opsForZSet().add(
                    WAITING_USERS_KEY,
                    user1.getUserId(),
                    -user1.getStartTime()
            );
            redisTemplate.opsForZSet().add(
                    WAITING_USERS_KEY,
                    user2.getUserId(),
                    -user2.getStartTime()
            );

            log.info("매칭 상태 롤백 완료: {} - {}", user1.getUserId(), user2.getUserId());
        } catch (Exception e) {
            log.error("매칭 상태 롤백 중 오류 발생", e);
        }
    }

    /**
     * 매칭 응답 처리
     * 매칭된 사용자의 수락/거절 처리
     */
    @Override
    public void processMatchResponse(MatchResponse response) {
        try {
            String userKey = USER_KEY_PREFIX + response.getUserId();
            UserMatchStatus user = getUserStatus(response.getUserId());

            if (user == null || user.getMatchId() == null) {
                log.warn("잘못된 매칭 응답: {}", response.getUserId());
                return;
            }

            if (response.isAccepted()) {
                processAcceptance(user);
            } else {
                processRejection(user);
            }
        } catch (Exception e) {
            log.error("매칭 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * 매칭 수락 처리
     * 양측 모두 수락 시 채팅방 생성
     */
    private void processAcceptance(UserMatchStatus user) {
        user.setAccepted(true);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);

        if (checkBothAccepted(user.getMatchId())) {
            createChatRoom(user.getMatchId());
            cleanupMatch(user.getMatchId());
        }
    }

    /**
     * 매칭 거절 처리
     * 상대방에게 알림 전송 및 상태 초기화
     */
    private void processRejection(UserMatchStatus user) {
        try {
            String matchId = user.getMatchId();
            UserMatchStatus otherUser = findOtherUser(matchId, user.getUserId());

            if (otherUser != null) {
                // 양측 상태 초기화 및 재매칭 큐에 추가
                resetUsers(user, otherUser);
                notifyUser(otherUser.getUserId(), "MATCH_REJECTED",
                        "매칭이 거절되었습니다. 새로운 매칭을 시작합니다.");
            }

            log.info("매칭 거절 처리 완료: {}", user.getUserId());
        } catch (Exception e) {
            log.error("매칭 거절 처리 중 오류 발생", e);
        }
    }

    /**
     * 매칭 실패 시 유저들의 상태 초기화
     * 1. 상태를 다시 WAITING으로 변경
     * 2. 매칭 ID 및 수락 상태 초기화
     * 3. Redis 상태 업데이트
     * 4. 다시 매칭 큐에 추가
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
        long currentTime = System.currentTimeMillis();
        user1.setStartTime(currentTime);
        user2.setStartTime(currentTime);

        // Redis 상태 업데이트
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user1.getUserId(), user1);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user2.getUserId(), user2);

        // 대기열에 재추가
        redisTemplate.opsForZSet().add(WAITING_USERS_KEY, user1.getUserId(), -currentTime);
        redisTemplate.opsForZSet().add(WAITING_USERS_KEY, user2.getUserId(), -currentTime);
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

        for (String userId : userIds) {
            UserMatchStatus user = (UserMatchStatus) redisTemplate.opsForValue().get("user:" + userId);
            if (!user.isAccepted()) return false;
        }
        return true;
    }

    private void extendWaitingTime(UserMatchStatus user) {
        long extendedTime = System.currentTimeMillis() + TIMEOUT_THRESHOLD;
        user.setStartTime(extendedTime);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);
        redisTemplate.opsForZSet().add(WAITING_USERS_KEY, user.getUserId(), -extendedTime);

        notifyUser(user.getUserId(), "WAITING_EXTENDED",
                "대기 시간이 추가로 연장되었습니다.");
    }

    private void moveToNextSession(UserMatchStatus user) {
        // 현재 세션에서 제거
        redisTemplate.opsForZSet().remove(WAITING_USERS_KEY, user.getUserId());
        redisTemplate.delete(USER_KEY_PREFIX + user.getUserId());

        // 다음 세션 정보 전송
        notifyUser(user.getUserId(), "NEXT_SESSION",
                "다음 매칭 세션으로 이동되었습니다.");
    }


    /**
     * Web socket을 통한 단일 유저에게 매칭 상태 전송
     * 지정된 유저에게 특정 메시지 타입과 내용을 포함해 전송
     */
    private void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/matching/" + userId,
                new MatchResponseDto(type, message, null)
        );
    }

    /**
     * 매칭 성사 알림 전송 (수락 여부를 응답 받기 위한 알림)
     * 1. 매칭 ID를 포함한 데이터 준비
     * 2. 매칭된 두 유저에게 성공 메시지 전송
     */
    private void notifyMatchSuccess(String user1Id, String user2Id, String matchId) {
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
     * 타임아웃된 사용자에게 선택지 제공
     * 1. 추가 대기
     * 2. 매칭 조건 완화
     * 3. 다음 세션으로 이동
     */
    private void notifyTimeoutOptions(UserMatchStatus user) {
        Map<String, Object> options = Map.of(
                "type", "TIMEOUT_OPTIONS",
                "options", Arrays.asList(
                        "추가 대기 (예상: 5-10분)",
                        "매칭 조건 완화 (MBTI 무관)",
                        "다음 세션으로 이동"
                ),
                "waitingTime", System.currentTimeMillis() - user.getStartTime()
        );

        messagingTemplate.convertAndSend(
                "/topic/matching/" + user.getUserId(),
                new MatchResponseDto("TIMEOUT", "매칭 대기 시간 초과", options)
        );
    }

    /**
     * 사용자 타임아웃 선택 처리
     * 선택에 따른 후속 작업 수행
     */
    public void handleTimeoutChoice(String userId, String choice) {
        UserMatchStatus user = getUserStatus(userId);
        if (user == null) return;

        switch (choice) {
            case "WAIT":
                // 대기 시간 연장
                extendWaitingTime(user);
                break;
            case "RELAX":
                // 매칭 조건 완화하여 재시도
                relaxMatchingCriteria(user);
                break;
            case "NEXT":
                // 다음 세션으로 이동
                moveToNextSession(user);
                break;
        }
    }

    /**
     * 매칭 조건 완화 처리
     * MBTI 선호도 제거 및 즉시 재매칭
     */
    private void relaxMatchingCriteria(UserMatchStatus user) {
        user.setPreferredMbti(null);  // MBTI 선호도 제거
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getUserId(), user);

        // 즉시 재매칭 시도
        matchingQueue.offer(new MatchingTask(user.getUserId()));

        notifyUser(user.getUserId(), "CRITERIA_RELAXED",
                "매칭 조건이 완화되었습니다. 새로운 매칭을 시도합니다.");
    }

    /**
     * 유틸리티 메서드: Redis에서 사용자 상태 조회
     */
    private UserMatchStatus getUserStatus(String userId) {
        return (UserMatchStatus) redisTemplate.opsForValue()
                .get(USER_KEY_PREFIX + userId);
    }

    /**
     * 유틸리티 메서드: 매칭된 상대방 찾기
     */
    private UserMatchStatus findOtherUser(String matchId, String userId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue()
                .get(MATCH_KEY_PREFIX + matchId);

        if (userIds == null) return null;

        return userIds.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .map(this::getUserStatus)
                .orElse(null);
    }

    /**
     * 매칭 후 채팅방 생성 요청
     */
    private void createChatRoom(String matchId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue()
                .get(MATCH_KEY_PREFIX + matchId);

        if (userIds == null) return;

        // 채팅 서버에 방 생성 요청
        chatServiceClient.post()
                .uri("/rooms")
                .bodyValue(Map.of("userIds", userIds))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        null,
                        error -> log.error("채팅방 생성 중 오류 발생")
                );
    }

    /**
     * 매칭 데이터 정리
     */
    private void cleanupMatch(String matchId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue()
                .get(MATCH_KEY_PREFIX + matchId);

        if (userIds != null) {
            for (String userId : userIds) {
                redisTemplate.delete(USER_KEY_PREFIX + userId);
            }
        }
        redisTemplate.delete(MATCH_KEY_PREFIX + matchId);
    }

    // 내부 클래스 정의
    @Data
    @AllArgsConstructor
    private static class MatchingTask {
        private String userId;
    }

    @Data
    @AllArgsConstructor
    private static class MatchScore {
        private UserMatchStatus user1;
        private UserMatchStatus user2;
        private double score;
    }
}



