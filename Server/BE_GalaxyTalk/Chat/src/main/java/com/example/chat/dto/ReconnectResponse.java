package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReconnectResponse {
    String chatRoomId;
    String sessionId;
    String token;
}
