package com.galaxytalk.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseDto {

    private boolean success;
    private String message;
    private Object data;


    private ApiResponseDto(String message) {
        this.success = false;
        this.message = message;
        this.data = null;
    }

    public static final ApiResponseDto forbidden = new ApiResponseDto("권한이 없습니다.");
    public static final ApiResponseDto noAccessToken = new ApiResponseDto("엑세스 토큰이 없습니다");
}


