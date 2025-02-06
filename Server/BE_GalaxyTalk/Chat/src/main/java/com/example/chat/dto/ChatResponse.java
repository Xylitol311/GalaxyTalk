package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String sessionId;    // OpenVidu 세션 ID
    private String tokenA;       // 첫 번째 사용자의 토큰
    private String tokenB;       // 두 번째 사용자의 토큰
    private String chatRoomId;   // 채팅방 ID
}