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
public class MatchProcessor {

    private final RedisService redisService;
    private final WebSocketService webSocketService;
    private final ExternalApiService externalApiService;

    /**
     * 매칭 생성 및 후속 처리
     * 1. 매칭 ID 생성 및 유저 상태 업데이트
     * 2. 매칭 알림 전송 (비동기: 3초 후)
     * 3. 1분 내 수락 없을 경우 자동 취소 로직 실행
     *
     * @param user1 첫번째 유저
     * @param user2 두번째 유저
     * @param similarity 두 유저 간 유사도
     */
    public void createMatch(UserMatchStatus user1, UserMatchStatus user2, double similarity) {
        // 1. 매칭 ID 생성 및 상태 업데이트
        String matchId = UUID.randomUUID().toString();
        similarity = (double) Math.round(similarity * 10000) / 100.0;
        updateMatchStatus(user1, user2, matchId, similarity);

        // 2. 대기 큐에서 해당 유저 제거
        redisService.removeUserFromWaitingQueue(user1.getUserId());
        redisService.removeUserFromWaitingQueue(user2.getUserId());

        // 2-1. 매칭 대상 대기 큐 나감 전체 알림
        webSocketService.broadcastUserExit(user1.getUserId());
        webSocketService.broadcastUserExit(user2.getUserId());

        // 3. 매칭 성공 알림 전송 (3초 후)
        sendMatchNotification(user1, user2, matchId, similarity);

        // 4. 1분 내 수락 응답이 없으면 매칭 취소 처리
        scheduleMatchCancellation(matchId);
    }

    /**
     * 유저들의 상태를 MATCHED로 업데이트하고, 매칭 정보를 Redis에 저장합니다.
     *
     * @param user1 첫번째 유저
     * @param user2 두번째 유저
     * @param matchId 생성된 매칭 ID
     * @param similarity 두 유저 간 유사도
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
     * 3초 후에 매칭 알림을 전송하는 함수.
     *
     * @param user1 첫번째 유저
     * @param user2 두번째 유저
     * @param matchId 매칭 ID
     * @param similarity 유사도
     */
    private void sendMatchNotification(UserMatchStatus user1, UserMatchStatus user2, String matchId, double similarity) {
        Map<String, Object> user1Data = new HashMap<>();
        user1Data.put("userId", user1.getUserId());
        user1Data.put("matchId", matchId);
        user1Data.put("matchUserId", user2.getUserId());
        user1Data.put("concern", user2.getConcern());
        user1Data.put("mbti", user2.getMbti());
        user1Data.put("energy", user2.getEnergy());
        user1Data.put("similarity", similarity);

        Map<String, Object> user2Data = new HashMap<>();
        user2Data.put("userId", user2.getUserId());
        user2Data.put("matchId", matchId);
        user2Data.put("matchUserId", user1.getUserId());
        user2Data.put("concern", user1.getConcern());
        user2Data.put("mbti", user1.getMbti());
        // user1의 energy 값을 저장하도록 수정
        user2Data.put("energy", user1.getEnergy());
        user2Data.put("similarity", similarity);

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
                .execute(() -> webSocketService.notifyMatch(user1Data, user2Data));
    }

    /**
     * 1분 후에 두 유저가 모두 수락했는지 확인하고, 수락하지 않은 경우 매칭을 취소하는 로직을 예약합니다.
     *
     * @param matchId 매칭 ID
     */
    private void scheduleMatchCancellation(String matchId) {
        CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(() -> {
            if (!checkBothAccepted(matchId)) {
                log.info("매칭 {} 1분 내 수락 미응답으로 취소 처리", matchId);
                cancelMatch(matchId);
            }
        });
    }

    /**
     * 매칭 취소 처리
     * - 각 유저의 상태를 WAITING으로 초기화 후 Redis 업데이트 및 대기 큐 재등록
     * - 취소 알림 전송 및 매칭 정보 삭제
     *
     * @param matchId 매칭 ID
     */
    private void cancelMatch(String matchId) {
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        if (matchResultStatus == null) {
            return;
        }
        for (String uid : matchResultStatus.getUserIds()) {
            UserMatchStatus user = redisService.getUserStatus(uid);
            if (user != null && user.getStatus() == MatchStatus.MATCHED) {
                resetUserState(user);
                webSocketService.notifyUser(uid, "CANCEL_MATCHED", "매칭 성사 취소");
            }
        }
        redisService.deleteMatchInfo(matchId);
    }

    /**
     * 매칭된 유저의 상태를 초기화하여 WAITING 상태로 되돌리고, 대기 큐에 재등록합니다.
     *
     * @param user 매칭 취소 대상 유저
     */
    private void resetUserState(UserMatchStatus user) {
        user.setStatus(MatchStatus.WAITING);
        user.setMatchId(null);
        user.setAccepted(false);
        redisService.saveUserStatus(user);
        redisService.addUserToWaitingQueue(user);
        webSocketService.broadcastNewUser(user);
    }

    /**
     * 매칭 수락/거절 응답 처리
     * 응답에 따라 수락 또는 거절 프로세스를 분기합니다.
     *
     * @param user 응답한 유저
     * @param response 매칭 수락/거절 요청 DTO
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
     * - 응답한 유저의 수락 상태 업데이트
     * - 양쪽 모두 수락한 경우 채팅방 생성 및 매칭 정보 정리
     *
     * @param user 수락 응답 유저
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
     * - 매칭 정보를 바탕으로 두 유저의 채팅방을 생성하고, 상태를 "Chatting"으로 업데이트
     * - 채팅방 정보 알림을 전송
     *
     * @param user 매칭 응답 유저 (해당 매칭에 포함된 두 유저 모두 대상으로 처리)
     */
    private void createChat(UserMatchStatus user) {
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(user.getMatchId());
        if (matchResultStatus == null || matchResultStatus.getUserIds().size() < 2) {
            log.warn("채팅 생성 실패: 매칭 정보가 올바르지 않습니다. matchId={}", user.getMatchId());
            return;
        }
        UserMatchStatus user1 = redisService.getUserStatus(matchResultStatus.getUserIds().get(0));
        UserMatchStatus user2 = redisService.getUserStatus(matchResultStatus.getUserIds().get(1));

        Map<String, Object> chatResponse = externalApiService.createChatRoom(user1, user2, matchResultStatus.getSimilarity());

        externalApiService.setUserStatus(user1.getUserId(), "Chatting");
        externalApiService.setUserStatus(user2.getUserId(), "Chatting");

        webSocketService.notifyUsersWithChatRoom(user1, user2, chatResponse);
    }

    /**
     * 매칭 거절 처리
     * - 거절한 유저의 상대방을 찾아 상태 초기화 및 재등록
     * - 상대방에게 매칭 실패 알림 전송
     *
     * @param user 거절 응답 유저
     */
    private void processRejection(UserMatchStatus user) {
        String matchId = user.getMatchId();
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        if (matchResultStatus == null || matchResultStatus.getUserIds() == null) {
            return;
        }
        // user가 거절한 상대방 id 추출
        String otherUserId = matchResultStatus.getUserIds().stream()
                .filter(id -> !id.equals(user.getUserId()))
                .findFirst()
                .orElse(null);

        if (otherUserId != null) {
            UserMatchStatus otherUser = redisService.getUserStatus(otherUserId);
            if (otherUser != null) {
                // 두 유저 상태 초기화
                resetUsers(user, otherUser);
                webSocketService.notifyUser(otherUserId, "MATCH_FAILED", "상대방이 매칭을 거절했습니다.");
            }
        }
    }

    /**
     * 두 유저의 상태를 초기화하여 WAITING 상태로 재설정하고, 대기 큐에 재등록합니다.
     *
     * @param user1 거절한 유저
     * @param user2 상대방 유저
     */
    private void resetUsers(UserMatchStatus user1, UserMatchStatus user2) {
        // WAITING 상태로 변경
        user1.setStatus(MatchStatus.WAITING);
        user2.setStatus(MatchStatus.WAITING);

        // 매칭 아이디, 수락 상태 초기화
        user1.setMatchId(null);
        user2.setMatchId(null);
        user1.setAccepted(false);
        user2.setAccepted(false);

        // 유저 상태 업데이트
        redisService.saveUserStatus(user1);
        redisService.saveUserStatus(user2);

        // 대기 큐에 추가
        redisService.addUserToWaitingQueue(user1);
        redisService.addUserToWaitingQueue(user2);

        // 브로드 캐스팅
        webSocketService.broadcastNewUser(user1);
        webSocketService.broadcastNewUser(user2);
    }

    /**
     * 매칭 성공 후, 매칭 정보를 정리하기 위한 로직
     * - 매칭된 유저들의 상태 정보를 Redis에서 삭제하고, 매칭 정보를 제거합니다.
     *
     * @param matchId 매칭 ID
     */
    private void cleanupMatch(String matchId) {
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        if (matchResultStatus == null || matchResultStatus.getUserIds() == null) {
            return;
        }
        for (String userId : matchResultStatus.getUserIds()) {
            redisService.deleteUserStatus(userId);
        }
        redisService.deleteMatchInfo(matchId);
    }

    /**
     * 매칭된 두 유저가 모두 수락했는지 검사합니다.
     *
     * @param matchId 매칭 ID
     * @return 두 유저 모두 수락한 경우 true, 그렇지 않으면 false
     */
    private boolean checkBothAccepted(String matchId) {
        MatchResultStatus matchResultStatus = redisService.getMatchInfo(matchId);
        if (matchResultStatus == null || matchResultStatus.getUserIds() == null) {
            return false;
        }
        return matchResultStatus.getUserIds().stream()
                .map(redisService::getUserStatus)
                .filter(Objects::nonNull)
                .allMatch(UserMatchStatus::isAccepted);
    }
}
