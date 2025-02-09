package com.example.match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {
    private String userId;
    private int energy;
    private Role role;
    private String mbti;
    private int planetId;

    @Override
    public String toString() {
        return "UserResponseDto{" +
                "userId='" + userId + '\'' +
                ", energy=" + energy +
                ", role=" + role +
                ", mbti='" + mbti + '\'' +
                ", planetId=" + planetId +
                '}';
    }
}
