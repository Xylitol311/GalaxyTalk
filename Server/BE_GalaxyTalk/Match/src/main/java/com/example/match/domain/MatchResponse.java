package com.example.match.domain;

import lombok.Data;

@Data
public class MatchResponse {
    private String userId;
    private String matchId;
    private boolean accepted;
}
