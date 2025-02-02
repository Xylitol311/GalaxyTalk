package com.example.match.service;

import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class MatchService {
    private final RedisTemplate<String, Object> redisTemplate; // redis
    private final SimpMessagingTemplate messagingTemplate; // web socket

    // 외부 API 호출을 위한 RestTemplate
//    private final RestTemplate restTemplate;

    // 외부 서버 API 엔드포인트 상수 정의
    private static final String AI_SERVER_URL = "http://ai-server/similarity";
    private static final String CHAT_SERVER_URL = "http://chat-server/room";
    // 매칭 성사를 위한 최소 유사도 임계값
    private static final double SIMILARITY_THRESHOLD = 0.7;

    // 매칭 대기 중인 유저들의 id를 저장하는 큐
    private final Queue<String> matchingQueue = new ConcurrentLinkedQueue<>();

    /**
     * 매칭 시작 처리
     * 1. 유저 상태 redis에 저장
     * 2. 매칭 큐에 유저 추가
     * 3. Web socket으로 대기 시작 알림
     */
    public void startMatching(UserMatchStatus user) {
        // Redis에 유저 상태 저장
        String userKey = "user:" + user.getUserId();
        user.setStatus(MatchStatus.WAITING);
        redisTemplate.opsForValue().set(userKey, user);

        // 매칭 큐에 추가
        matchingQueue.offer(user.getUserId());

        // WebSocket으로 대기 상태 알림
        notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");
    }

    /**
     * 5초마다 주기적으로 실행되는 매칭 처리 로직
     * 1. 대기 중인 유저들을 조회
     * 2. 모든 가능한 유저 쌍에 대해 유사도 계산
     * 3. 임계값을 넘는 첫 번째 쌍을 매칭
     */
    @Scheduled(fixedRate = 5000)
    public void processMatching() {
        List<UserMatchStatus> waitingUsers = findWaitingUsers();
        if (waitingUsers.size() < 2) return;

        // 모든 가능한 유저 쌍에 대해 유사도 검사
        for (int i = 0; i < waitingUsers.size() - 1; i++) {
            for (int j = i + 1; j < waitingUsers.size(); j++) {
                UserMatchStatus user1 = waitingUsers.get(i);
                UserMatchStatus user2 = waitingUsers.get(j);

                double similarity = calculateSimilarity(user1, user2);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    createMatch(user1, user2);
                    return;
                }
            }
        }
    }

    /**
     * 매칭 큐에서 실제 대기 중인 유저들만 필터링하여 조회
     * Redis에서 각 유저의 현재 상태를 확인하여 WAITING 상태인 유저만 반환
     */
    private List<UserMatchStatus> findWaitingUsers() {
        List<UserMatchStatus> waitingUsers = new ArrayList<>();
        for (String userId : matchingQueue) {
            String userKey = "user:" + userId;
            UserMatchStatus user = (UserMatchStatus) redisTemplate.opsForValue().get(userKey);
            if (user != null && user.getStatus() == MatchStatus.WAITING) {
                waitingUsers.add(user);
            }
        }
        return waitingUsers;
    }

    /**
     * AI 서버에 두 유저 간의 유사도 점수 계산 요청
     * 고민 내용, MBTI, 나이 등을 기반으로 유사도 계산
     *
     * !!!RestTemplate 관련 코드 구현 후 수정 필요!!!
     */
    private double calculateSimilarity(UserMatchStatus user1, UserMatchStatus user2) {
        Map<String, Object> request = Map.of(
                "user1", user1,
                "user2", user2
        );

//        ResponseEntity<Double> response = restTemplate.postForEntity(
//                AI_SERVER_URL,
//                request,
//                Double.class
//        );

//        return response.getBody() != null ? response.getBody() : 0.0;
        return 0.0;
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

        resetUsers(user, otherUser);
        notifyUser(user.getUserId(), "MATCH_FAILED", "상대방이 매칭을 거절했습니다.");
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

    /**
     * 매칭된 상대방 유저 정보 조회
     * 1. 매칭 ID로 매칭된 유저 목록 조회
     * 2. 현재 유저를 제외한 상대방 유저 ID 필터링
     * 3. 상대방 유저의 상태 정보 반환
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
     * 매칭 실패 시 유저들의 상태 초기화
     * 1. 상태를 다시 WAITING으로 변경
     * 2. 매칭 ID 및 수락 상태 초기화
     * 3. Redis 상태 업데이트
     * 4. 다시 매칭 큐에 추가
     */
    private void resetUsers(UserMatchStatus user1, UserMatchStatus user2) {
        user1.setStatus(MatchStatus.WAITING);
        user2.setStatus(MatchStatus.WAITING);
        user1.setMatchId(null);
        user2.setMatchId(null);
        user1.setAccepted(false);
        user2.setAccepted(false);

        redisTemplate.opsForValue().set("user:" + user1.getUserId(), user1);
        redisTemplate.opsForValue().set("user:" + user2.getUserId(), user2);

        matchingQueue.offer(user1.getUserId());
        matchingQueue.offer(user2.getUserId());
    }

    /**
     * 매칭 성공 시 채팅방 생성 요청
     * 1. 매칭된 유저 목록 조회
     * 2. 채팅 서버에 채팅방 생성 요청
     *
     * !!!RestTemplate 관련 코드 구현 후 수정 필요!!!
     */
    private void createChatRoom(String matchId) {
        List<String> userIds = (List<String>) redisTemplate.opsForValue().get("match:" + matchId);
        if (userIds == null) return;

        Map<String, Object> request = Map.of("userIds", userIds);
//        restTemplate.postForEntity(CHAT_SERVER_URL, request, Void.class);
    }

    /**
     * 매칭 완료 후 관련 데이터 정리
     * 1. 매칭된 유저들의 Redis 데이터 삭제
     * 2. 매칭 정보 삭제
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