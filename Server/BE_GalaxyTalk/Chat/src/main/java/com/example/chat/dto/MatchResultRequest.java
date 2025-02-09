package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class MatchResultRequest {
    String userId1;
    String userId2;
    String concern1;
    String concern2;
    Double similarityScore;
}
