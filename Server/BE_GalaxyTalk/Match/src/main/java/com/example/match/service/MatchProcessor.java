package com.example.match.service;

import com.example.match.domain.MatchResultStatus;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchApproveRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * 매칭 생성 및 처리 관련 로직 담당
 * - 매칭 수락/거절 처리
 * - 유저 상태 관리
 * - 매칭 정보 정리
 */
public class MatchProcessor {
    private final RedisService redisService;
    private final WebSocketService webSocketService;
    private final ExternalApiService externalApiService;
    // 기존 queueManager 관련 코드는 새 로직에서는 사용하지 않습니다.

    /**
     * 두 유저 간의 매칭 생성
     * 1. 매칭 ID 생성
     * 2. 두 유저의 상태 업데이트
     * 3. 매칭 성사 알림 전송 (5초 후에 notifyMatch 호출)
     */
    public void createMatch(UserMatchStatus user1, UserMatchStatus user2, double similarity) {
        String matchId = UUID.randomUUID().toString();
        updateMatchStatus(user1, user2, matchId, similarity);

        // 매칭 성공 시, 대기 큐(Redis Waiting Pool)에서 해당 유저 제거
        redisService.removeUserFromWaitingQueue(user1.getUserId());
        redisService.removeUserFromWaitingQueue(user2.getUserId());

        Map<String, Object> user1Data = new HashMap<>();
        user1Data.put("userId", user1.getUserId());
        user1Data.put("matchId", matchId);
        user1Data.put("matchUserId", user2.getUserId());
        user1Data.put("concern", user2.getConcern());
        user1Data.put("mbti", user2.getMbti());
        // energy 등 추가 정보가 있다면 포함할 수 있음
        user1Data.put("similarity", similarity);

        Map<String, Object> user2Data = new HashMap<>();
        user2Data.put("userId", user2.getUserId());
        user2Data.put("matchId", matchId);
        user2Data.put("matchUserId", user1.getUserId());
        user2Data.put("concern", user1.getConcern());
        user2Data.put("mbti", user1.getMbti());
        user2Data.put("similarity", similarity);

        // 5초 후에 매칭 알림 전송 (비동기 처리)
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
                .execute(() -> webSocketService.notifyMatch(user1Data, user2Data));
    }

    /**
     * 매칭된 유저들의 상태 정보를 업데이트합니다.
     * 1. 각 유저의 상태를 MATCHED로 변경
     * 2. 매칭 ID 설정
     * 3. Redis에 업데이트된 정보를 저장
     */
    private void updateMatchStatus(UserMatchStatus user1, UserMatchStatus user2, String matchId, double similarity) {
        user1.setStatus(MatchStatus.MATCHED);
        user2.setStatus(MatchStatus.MATCHED);
        user1.setMatchId(matchId);
        user2.setMatchId(matchId);

        MatchResultStatus matchResult = new MatchResultStatus();
        matchResult.setUserIds(List.of(user1.getUserId(), user2.getUserId()));
        matchResult.setSimilarity(similarity);

        redisService.saveUserStatus(user1);
        redisService.saveUserStatus(user2);
        redisService.saveMatchInfo(matchId, matchResult);
    }

    /**
     * 매칭 수락/거절 응답 처리
     * 유저의 응답에 따라 수락 또는 거절 프로세스를 실행합니다.
     */
    public void processMatchResponse(UserMatchStatus user, MatchApproveRequestDto response) {
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
        redisService.saveUserStatus(user);

        if (checkBothAccepted(user.getMatchId())) {
            createChat(user);
            cleanupMatch(user.getMatchId());
        }
    }

    /**
     * 채팅방 생성
     * - 유저 정보와 매칭 정보, 유사도 점수를 담아 채팅방 생성 요청
     * - 채팅 서비스로부터 sessionId, token, chatRoomId 등을 받아서 사용자에게 전달
     * - 유저 세션 서버에 유저 상태를 "Chatting"으로 변경 요청
     */
    private void createChat(UserMatchStatus user) {
        // 매칭 정보 조회
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(user.getMatchId());
        UserMatchStatus user1 = redisService.getUserStatus(matchResultStatus.getUserIds().get(0));
        UserMatchStatus user2 = redisService.getUserStatus(matchResultStatus.getUserIds().get(1));

        // 채팅방 생성 요청 (외부 API 호출)
        Map<String, Object> chatResponse = externalApiService.createChatRoom(user1, user2, matchResultStatus.getSimilarity());

        // 유저 상태 변경 요청: "Chatting"
        externalApiService.setUserStatus(user1.getUserId(), "Chatting");
        externalApiService.setUserStatus(user2.getUserId(), "Chatting");

        // 채팅방 정보 알림 전송
        webSocketService.notifyUsersWithChatRoom(user1, user2, chatResponse);
    }

    /**
     * 매칭된 두 유저가 모두 수락했는지 확인합니다.
     * 1. 매칭 ID로 관련 유저 목록을 조회
     * 2. 각 유저의 수락 상태를 확인하여 모두 수락했으면 true 반환
     */
    private boolean checkBothAccepted(String matchId) {
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        List<String> userIds = matchResultStatus.getUserIds();
        if (userIds == null) return false;
        return userIds.stream()
                .map(redisService::getUserStatus)
                .filter(Objects::nonNull)
                .allMatch(UserMatchStatus::isAccepted);
    }

    /**
     * 매칭 거절 처리
     * - 한 유저가 거절한 경우, 양쪽 유저의 상태를 초기화하고 대기 큐에 재등록하여 재매칭을 시도합니다.
     */
    private void processRejection(UserMatchStatus user) {
        String matchId = user.getMatchId();
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        List<String> userIds = matchResultStatus.getUserIds();

        if (userIds != null) {
            String otherUserId = userIds.stream()
                    .filter(id -> !id.equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);
            if (otherUserId != null) {
                UserMatchStatus otherUser = redisService.getUserStatus(otherUserId);
                if (otherUser != null) {
                    // 상태 초기화 및 재등록
                    resetUsers(user, otherUser);
                    webSocketService.notifyUser(otherUserId, "MATCH_FAILED", "상대방이 매칭을 거절했습니다.");
                }
            }
        }
    }

    /**
     * 매칭 실패(거절) 시 유저들의 상태를 초기화하고 Redis 대기 큐에 재등록합니다.
     */
    private void resetUsers(UserMatchStatus user1, UserMatchStatus user2) {
        // 상태 초기화
        user1.setStatus(MatchStatus.WAITING);
        user2.setStatus(MatchStatus.WAITING);
        user1.setMatchId(null);
        user2.setMatchId(null);
        user1.setAccepted(false);
        user2.setAccepted(false);

        // Redis 상태 업데이트 및 대기 큐 재등록
        redisService.saveUserStatus(user1);
        redisService.saveUserStatus(user2);
        redisService.addUserToWaitingQueue(user1);
        redisService.addUserToWaitingQueue(user2);
    }

    /**
     * 매칭 성공 시, 매칭 정보 정리를 위해
     * 매칭된 유저들의 정보를 Redis에서 삭제하고 매칭 정보를 제거합니다.
     */
    private void cleanupMatch(String matchId) {
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        List<String> userIds = matchResultStatus.getUserIds();
        if (userIds == null) return;
        for (String userId : userIds) {
            redisService.deleteUserStatus(userId);
        }
        redisService.deleteMatchInfo(matchId);
    }
}
