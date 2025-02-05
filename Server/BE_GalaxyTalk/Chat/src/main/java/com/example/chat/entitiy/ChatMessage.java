package com.example.chat.entitiy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    @Id
    private String id;
    private String sessionId;    // OpenVidu 세션 ID
    private String senderId;     // 메시지 발신자 ID
    private String content;      // 메시지 내용
    private LocalDateTime timestamp;  // 메시지 전송 시간
}