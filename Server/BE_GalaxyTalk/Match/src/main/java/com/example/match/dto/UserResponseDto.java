package com.example.match.dto;

import lombok.*;

@Data
public class UserResponseDto {
    private boolean success;
    private String message;
    private UserSendDTO data;


    @Data
    public static class UserSendDTO {
        private String userId;
        private String mbti;
        private int energy;
        private Role role;
        private int planetId;
    }

}
