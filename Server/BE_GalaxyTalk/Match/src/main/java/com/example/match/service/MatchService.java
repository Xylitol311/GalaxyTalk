package com.example.match.service;

import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class MatchService {
    private final RedisTemplate<String, Object> redisTemplate; // redis
    private final SimpMessagingTemplate messagingTemplate; // web socket

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
     * Web socket을 통한 단일 유저에게 매칭 상태 전송
     * 지정된 유저에게 특정 메시지 타입과 내용을 포함해 전송
     */
    private void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/matching/" + userId,
                new MatchResponseDto(type, message, null)
        );
    }
}
