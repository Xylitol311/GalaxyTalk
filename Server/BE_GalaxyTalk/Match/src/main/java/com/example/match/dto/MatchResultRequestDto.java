package com.example.match.dto;

import lombok.Data;

@Data
public class MatchResultRequestDto {
    String userId1;
    String userId2;
    String concern1;
    String concern2;
    Double similarityScore;
}