package com.example.match.dto;

import lombok.Data;

@Data
public class MatchRequestDto {
    private String userId;
    private String concern;
    private String preferredMbti;
    private int age;
}
