package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import com.example.match.dto.UserStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/matching/" + userId,
                new MatchResponseDto(type, message, null)
        );
    }

    public void notifyMatch(String user1Id, String user2Id, String matchId) {
        Map<String, Object> data = Map.of("matchId", matchId);
        String message = "매칭이 성사되었습니다.";

        messagingTemplate.convertAndSend(
                "/topic/matching/" + user1Id,
                new MatchResponseDto("MATCH_SUCCESS", message, data)
        );
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user2Id,
                new MatchResponseDto("MATCH_SUCCESS", message, data)
        );
    }

    public void broadcastNewUser(UserMatchStatus user) {
        UserStatusDto statusDto = new UserStatusDto(
                user.getUserId(),
                user.getConcern(),
                user.getMbti()
        );
        messagingTemplate.convertAndSend("/topic/matching/users/new", statusDto);
    }

    public void broadcastUserExit(String userId) {
        messagingTemplate.convertAndSend("/topic/matching/users/exit", userId);
    }
}
