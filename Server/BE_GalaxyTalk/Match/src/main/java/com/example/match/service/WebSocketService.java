package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MessageResponseDto;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;


    public void notifyUser(String userId, String type, String message) {
        log.info("유저 알림: {} - {} - {}", userId, type, message);
        if (userId == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "notifyUser: userId가 null입니다.");
        }
        messagingTemplate.convertAndSend(
                "/topic/matching/" + userId,
                new MessageResponseDto(type, message, null)
        );
    }


    public void notifyMatch(Map<String, Object> user1Data, Map<String, Object> user2Data) {
        log.info("매칭 알림: 사용자 데이터 {}, {}", user1Data, user2Data);
        String message = "매칭이 성사되었습니다.";
        // user1에게 전송
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user2Data.get("matchedUserId"),
                new MessageResponseDto("MATCH_SUCCESS", message, user1Data)
        );

        // user2에게 전송
        messagingTemplate.convertAndSend(
                "/topic/matching/" + user1Data.get("matchedUserId"),
                new MessageResponseDto("MATCH_SUCCESS", message, user2Data)
        );
    }

    public void notifyUsersWithChatRoom(UserMatchStatus user1, UserMatchStatus user2, Map<String, Object> chatResponse) {
        if (user1 == null || user2 == null || chatResponse == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "notifyUsersWithChatRoom: 인자가 null입니다.");
        }
        String message = "매칭이 완료되었습니다. 채팅방 정보입니다.";
        log.info("채팅방 생성 완료, 정보: {}", chatResponse);

        Map<String, Object> user1Data = new HashMap<>();
        user1Data.put("chatRoomId", chatResponse.get("chatRoomId"));
        user1Data.put("sessionId", chatResponse.get("sessionId"));
        user1Data.put("token", chatResponse.get("tokenA"));

        Map<String, Object> user2Data = new HashMap<>();
        user2Data.put("chatRoomId", chatResponse.get("chatRoomId"));
        user2Data.put("sessionId", chatResponse.get("sessionId"));
        user2Data.put("token", chatResponse.get("tokenB"));

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
        log.info("새로운 유저 입장 알림: {}", user);
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "broadcastNewUser: UserMatchStatus 혹은 userId가 null입니다.");
        }

        String message = "새로운 유저가 접속했습니다.";

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("concern", user.getConcern());
        data.put("mbti", user.getMbti());
        data.put("status", user.getStatus());
        data.put("startTime", user.getStartTime());
        messagingTemplate.convertAndSend("/topic/matching/users/new",
                new MessageResponseDto("NEW_USER", message, data));
    }

    public void broadcastUserExit(String userId) {
        log.info("유저 퇴장 알림: {}", userId);
        if (userId == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "broadcastUserExit: userId가 null입니다.");
        }
        String message = "해당 유저가 매칭 큐에서 제외되었습니다.";
        Map<String, Object> data = Map.of("userId", userId);
        messagingTemplate.convertAndSend("/topic/matching/users/exit",
                new MessageResponseDto("EXIT_USER", message, data));
    }
}
