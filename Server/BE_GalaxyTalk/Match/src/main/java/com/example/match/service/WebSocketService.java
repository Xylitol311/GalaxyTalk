package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.ChatRoomResponseDto;
import com.example.match.dto.MessageResponseDto;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUser(String userId, String type, String message) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "notifyUser: userId가 null입니다.");
        }
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

    public void notifyUsersWithChatRoom(UserMatchStatus user1, UserMatchStatus user2, ChatRoomResponseDto.ChatResponse chatResponse) {
        if (user1 == null || user2 == null || chatResponse == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "notifyUsersWithChatRoom: 인자가 null입니다.");
        }

        String message = "매칭이 완료되었습니다. 채팅방 정보입니다.";

        Map<String, Object> user1Data = new HashMap<>();
        user1Data.put("chatRoomId", chatResponse.getChatRoomId());
        user1Data.put("sessionId", chatResponse.getSessionId());
        user1Data.put("token", chatResponse.getTokenA());

        Map<String, Object> user2Data = new HashMap<>();
        user2Data.put("chatRoomId", chatResponse.getChatRoomId());
        user2Data.put("sessionId", chatResponse.getSessionId());
        user2Data.put("token", chatResponse.getTokenB());

        messagingTemplate.convertAndSend(
                "/topic/matching/" + user1.getUserId(),
                new MessageResponseDto("CHAT_CREATED", message, user1Data)
        );
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user2.getUserId(),
                new MessageResponseDto("CHAT_CREATED", message, user2Data)
        );
    }

    public void broadcastNewUser(UserMatchStatus user) {
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "broadcastNewUser: UserMatchStatus 혹은 userId가 null입니다.");
        }

        String message = "새로운 유저가 접속했습니다.";

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("concern", user.getConcern());
        data.put("mbti", user.getMbti());
        data.put("status", user.getStatus());

        messagingTemplate.convertAndSend("/topic/matching/users/new",
                new MessageResponseDto("NEW_USER", message, data));
    }

    public void broadcastUserExit(String userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "broadcastUserExit: userId가 null입니다.");
        }

        String message = "해당 유저가 매칭 큐에서 제외되었습니다.";
        Map<String, Object> data = Map.of("userId", userId);
        messagingTemplate.convertAndSend("/topic/matching/users/exit",
                new MessageResponseDto("EXIT_USER", message, data));
    }

}
