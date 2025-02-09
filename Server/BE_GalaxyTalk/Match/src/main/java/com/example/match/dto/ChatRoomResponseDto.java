package com.example.match.dto;

import lombok.Data;

@Data
public class ChatRoomResponseDto {
    private boolean success;
    private String message;
    private ChatRoomData data;

    @Data
    public static class ChatRoomData {
        private String sessionId;
        private String tokenA;
        private String tokenB;
        private String chatRoomId;
    }
}