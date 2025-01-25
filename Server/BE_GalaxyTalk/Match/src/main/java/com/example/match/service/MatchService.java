package com.example.match.service;

import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final Queue<String> matchingQueue = new ConcurrentLinkedQueue<>();

    public void startMatching(UserMatchStatus user) {
        // Redis에 유저 상태 저장
        String userKey = "user:" + user.getUserId();
        user.setStatus(MatchStatus.WAITING);
        redisTemplate.opsForValue().set(userKey, user);

        // 매칭 큐에 추가
        matchingQueue.offer(user.getUserId());

        // WebSocket으로 대기 상태 알림
        notifyUser(user.getUserId(), "WAITING", "매칭 대기 시작");

        // 매칭 함수 실행
         processMatching();
    }

    // 두 유저 선택해서 매칭 진행
    // 스케줄러로 주기적으로 파악해서 매칭 진행
    @Scheduled(fixedRate = 5000)
    public void processMatching() {
        // 매칭 큐에서 두 유저 선택
        if (matchingQueue.size() < 2) return;

        String user1Id = matchingQueue.poll();
        String user2Id = matchingQueue.poll();

        // redis에서 두 유저 정보 가져옴
        UserMatchStatus user1 = (UserMatchStatus) redisTemplate.opsForValue()
                .get("user:" + user1Id);
        UserMatchStatus user2 = (UserMatchStatus) redisTemplate.opsForValue()
                .get("user:" + user2Id);

        // AI 서버에 유사도 점수 요청
//        double similarity = calculateSimilarity(user1, user2);
        double similarity = 1;

        if (similarity >= 0.7) {
            // 매칭 id 랜덤으로 생성
            String matchId = UUID.randomUUID().toString();
            // 매칭 상태 업데이트
            updateMatchStatus(user1, user2, matchId);

            // 매칭 성공 알림 전송 -> 둘 다 수락 시 매칭 완료
            notifyMatch(user1.getUserId(), user2.getUserId(), matchId);
        } else {
            // 다시 큐에 추가
            matchingQueue.offer(user1Id);
            matchingQueue.offer(user2Id);
        }
    }

    // 유저 매칭 시 매칭 정보 업데이트
    private void updateMatchStatus(UserMatchStatus user1, UserMatchStatus user2, String matchId) {
        // 유저 매칭 상태 업데이트
        user1.setStatus(MatchStatus.MATCHED);
        user2.setStatus(MatchStatus.MATCHED);
        // 매칭된 id 정보 업데이트
        user1.setMatchId(matchId);
        user2.setMatchId(matchId);

        // redis에 유저 정보 업데이트
        redisTemplate.opsForValue().set("user:" + user1.getUserId(), user1);
        redisTemplate.opsForValue().set("user:" + user2.getUserId(), user2);
    }

    // 매칭 상태 전송
    private void notifyUser(String userId, String type, String message) {
//        messagingTemplate.convertAndSend(
//                "/topic/matching/" + userId,
//                new MatchResponseDto(type, message)
//        );
    }

    // 매칭 알림 전송
    private void notifyMatch(String user1Id, String user2Id, String matchId) {
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user1Id,
                Map.of("type", "MATCH_SUCCESS", "matchId", matchId)
        );
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user2Id,
                Map.of("type", "MATCH_SUCCESS", "matchId", matchId)
        );
    }
}
