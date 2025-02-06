package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    private String chatRoomId;   // 채팅방 ID
    private String senderId;     // 메시지 발신자 ID
    private String content;      // 메시지 내용
}
