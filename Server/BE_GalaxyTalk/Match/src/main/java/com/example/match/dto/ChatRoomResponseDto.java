package com.example.match.dto;

import lombok.Data;

@Data
public class ChatRoomResponseDto {
    private boolean success;
    private String message;
    private ChatResponse data;

    @Data
    public static class ChatResponse {
        private String sessionId;
        private String tokenA;
        private String tokenB;
        private String chatRoomId;
    }
}