package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParticipantInfo {
    String userId;
    String mbti;
    String concern;
    Integer planetId;
    Integer energy;
}
