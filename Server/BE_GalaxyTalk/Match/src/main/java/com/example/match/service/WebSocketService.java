package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MessageResponseDto;
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
                new MessageResponseDto(type, message, null)
        );
    }

    public void notifyMatch(Map<String, Object> user1Data, Map<String, Object> user2Data) {
        String message = "매칭이 성사되었습니다.";

        messagingTemplate.convertAndSend(
                "/topic/matching/" + user1Data.get("userId"),
                new MessageResponseDto("MATCH_SUCCESS", message, user1Data)
        );
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user2Data.get("userId"),
                new MessageResponseDto("MATCH_SUCCESS", message, user2Data)
        );
    }

    public void broadcastNewUser(UserMatchStatus user) {
        UserStatusDto statusDto = new UserStatusDto(
                user.getUserId(),
                user.getConcern(),
                user.getMbti(),
                user.getStatus()
        );
        messagingTemplate.convertAndSend("/topic/matching/users/new", statusDto);
    }

    public void broadcastUserExit(String userId) {
        messagingTemplate.convertAndSend("/topic/matching/users/exit", userId);
    }
}
