package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReconnectResponse {
    String sessionId;
    String token;
}
