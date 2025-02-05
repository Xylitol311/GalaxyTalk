package com.example.chat.controller;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ChatResponse;
import com.example.chat.entitiy.ChatMessage;
import com.example.chat.service.ChatService;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import java.util.List;

@RestController
@RequestMapping("/api/video")
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
    public ResponseEntity<ChatResponse> createSessionWithTwoTokens()
            throws OpenViduJavaClientException, OpenViduHttpException {

        // 세션 생성
        SessionProperties properties = new SessionProperties.Builder().build();
        Session session = openvidu.createSession(properties);
        String sessionId = session.getSessionId();

        // 첫 번째 사용자 토큰 생성
        ConnectionProperties propertiesA = new ConnectionProperties.Builder()
                .role(OpenViduRole.PUBLISHER)
                .build();
        Connection connectionA = session.createConnection(propertiesA);

        // 두 번째 사용자 토큰 생성
        ConnectionProperties propertiesB = new ConnectionProperties.Builder()
                .role(OpenViduRole.PUBLISHER)
                .build();
        Connection connectionB = session.createConnection(propertiesB);

        // DTO 응답 반환
        ChatResponse response = new ChatResponse(
                sessionId,
                connectionA.getToken(),
                connectionB.getToken()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 채팅 메시지 저장 API
     * @param chatRequest 저장할 메시지 정보
     * @return 저장된 메시지 객체
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessage> saveMessage(@RequestBody ChatRequest chatRequest) {
        ChatMessage savedMessage = chatService.saveMessage(chatRequest);
        return ResponseEntity.ok(savedMessage);
    }

    /**
     * 이전 채팅 내용 조회 API
     * @param sessionId OpenVidu 세션 ID
     * @return 해당 세션의 모든 메시지 리스트
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getPreviousMessages(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatService.getPreviousMessages(sessionId);
        return ResponseEntity.ok(messages);
    }
}
