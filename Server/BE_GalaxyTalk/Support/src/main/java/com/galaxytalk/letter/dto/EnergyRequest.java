package com.galaxytalk.letter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor  // 추가!
@Getter  // Lombok을 사용할 경우 필수
public class EnergyRequest {
    private String senderId;
    private String receiverId;
}


