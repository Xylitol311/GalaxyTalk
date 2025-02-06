package com.example.match.dto;

import lombok.Data;

@Data
public class MatchResponseRequestDto {
    private String userId;
    private String matchId;
    private boolean accepted;
}
