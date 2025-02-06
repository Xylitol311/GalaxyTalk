package com.example.chat.controller;

import com.example.chat.dto.*;
import com.example.chat.entity.ChatMessage;
import com.example.chat.entity.ChatRoom;
import com.example.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Value("${OPENVIDU_URL}")
    private String openviduUrl;

    @Value("${OPENVIDU_SECRET}")
    private String openviduSecret;

    private OpenVidu openvidu;

    private final ChatService chatService;

    @PostConstruct
    public void init() {
        this.openvidu = new OpenVidu(openviduUrl, openviduSecret);
    }

    // 세션(방) 생성 및 두 개의 토큰 발급 후 반환
    @PostMapping("/match")
    public ResponseEntity<ApiResponseDto> createSessionWithTwoTokens(@RequestBody MatchResultRequest matchResultRequest)
            throws OpenViduJavaClientException, OpenViduHttpException {
        
        // 세션 생성
        SessionProperties properties = new SessionProperties.Builder().build();
        Session session = openvidu.createSession(properties);
        String sessionId = session.getSessionId();

        // 채팅방을 생성하고 생성된 방의 ID를 가져옴
        String chatRoomId = chatService.createChatRoom(matchResultRequest, sessionId);

        // 첫 번째 사용자 토큰 생성
        String tokenA = generateToken(session);

        // 두 번째 사용자 토큰 생성
        String tokenB = generateToken(session);

        // DTO 응답 반환
        ChatResponse response = new ChatResponse(
                sessionId,
                tokenA,
                tokenB,
                chatRoomId
        );

        // 응답 예시 (200, ok)
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "연결 성공",
                response
        ));
    }

    /**
     * 채팅 메시지 저장 API
     * @param chatRequest 저장할 메시지 정보
     * @return 저장된 메시지 객체
     */
    @PostMapping("/{chatRoomId}/message")
    public ResponseEntity<ApiResponseDto> saveMessage(@RequestBody ChatRequest chatRequest, @PathVariable String chatRoomId) {
        chatRequest.setChatRoomId(chatRoomId);
        ChatMessage savedMessage = chatService.saveMessage(chatRequest);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "메세지 전송 성공",
                null
        ));
    }

    /**
     * 이전 채팅 내용 조회 API
     * @param chatRoomId 채팅방 ID
     * @return 해당 채팅방의 모든 메시지 리스트
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponseDto> getPreviousMessages(@PathVariable String chatRoomId) {
        List<ChatMessage> messages = chatService.getPreviousMessages(chatRoomId);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "이전 대화 조회 성공",
                messages
        ));
    }

    /**
     * 채팅방 나가기 API
     * @param chatRoomId 나갈 채팅방 ID
     * @return 채팅방 나가기 처리 결과
     */
    @DeleteMapping("/{chatRoomId}/leave")
    public ResponseEntity<ApiResponseDto> leaveChat(
            @PathVariable String chatRoomId) throws OpenViduJavaClientException, OpenViduHttpException {

        // 1. 채팅방의 세션 ID와 참여자 목록 조회
        ChatRoom chatRoom = chatService.getChatRoomWithParticipants(chatRoomId);
        Session session = openvidu.getActiveSession(chatRoom.getSessionId());

        if (session != null) {
            // 2. 세션의 모든 활성 연결 종료
            for (Connection connection : session.getActiveConnections()) {
                session.forceDisconnect(connection);
            }
        }

        // 3. 채팅방 종료 처리 (종료 시간 기록, 유저 상태 업데이트)
        chatService.endChatRoom(chatRoom);

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "채팅방 나가기 성공",
                null
        ));
    }

    /**
     * 채팅방 재연결
     * @param userId
     * @return sessionId, token
     */
    @PostMapping("/reconnect")
    public ResponseEntity<ApiResponseDto> reconnect(@RequestParam String userId) throws OpenViduJavaClientException, OpenViduHttpException {
        // 1. user가 속한 active session 찾기
        String sessionId = chatService.getSessionId(userId);

        // 2. 기존 세션 가져오기
        Session session = openvidu.getActiveSession(sessionId);

        // 3. 재연결 응답 객체 생성
        ReconnectResponse reconnectResponse = new ReconnectResponse(
                sessionId,
                generateToken(session)
        );

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "재연결 성공",
                reconnectResponse
        ));
    }

    /**
     * 세션과 property 정보를 받아 token을 생성합니다.
     */
    private String generateToken(Session session) throws OpenViduJavaClientException, OpenViduHttpException {
        ConnectionProperties properties = new ConnectionProperties.Builder()
                .role(OpenViduRole.PUBLISHER)
                .build();

        return session.createConnection(properties).getToken();
    }
}
