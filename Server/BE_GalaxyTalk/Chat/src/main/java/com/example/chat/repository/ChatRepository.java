package com.example.chat.repository;

import com.example.chat.entitiy.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends MongoRepository<ChatMessage, String> {
    /**
     * 특정 세션의 모든 메시지를 시간순으로 조회
     * @param sessionId OpenVidu 세션 ID
     * @return 해당 세션의 모든 메시지 리스트
     */
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
}