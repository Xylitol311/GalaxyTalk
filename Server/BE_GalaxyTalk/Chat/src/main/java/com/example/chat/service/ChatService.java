package com.example.chat.service;

import com.example.chat.dto.ChatRequest;
import com.example.chat.entitiy.ChatMessage;
import com.example.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository messageRepository;
    private final ChatRepository chatRepository;

    /**
     * 새로운 채팅 메시지를 저장
     * @param chatRequest 저장할 메시지 정보를 담은 DTO
     * @return 저장된 메시지 객체
     */
    public ChatMessage saveMessage(ChatRequest chatRequest) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(chatRequest.getSessionId());
        message.setSenderId(chatRequest.getSenderId());
        message.setContent(chatRequest.getContent());
        message.setTimestamp(LocalDateTime.now());

        log.info("Saving chat message for session {}: {}", chatRequest.getSessionId(), chatRequest.getContent());
        return messageRepository.save(message);
    }

    /**
     * 특정 세션의 이전 채팅 내용을 조회
     * @param sessionId OpenVidu 세션 ID
     * @return 해당 세션의 모든 메시지 리스트
     */
    public List<ChatMessage> getPreviousMessages(String sessionId) {
        log.info("Retrieving previous messages for session: {}", sessionId);
        return chatRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}