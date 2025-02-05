package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    private String sessionId;    // OpenVidu 세션 ID
    private String senderId;     // 메시지 발신자 ID
    private String content;      // 메시지 내용
}
