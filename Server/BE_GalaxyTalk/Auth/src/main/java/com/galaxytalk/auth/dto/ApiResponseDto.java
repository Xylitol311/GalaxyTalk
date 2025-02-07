package com.galaxytalk.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseDto {

    private boolean success;
    private String message;
    private Object data;

    public ApiResponseDto(Object data){
        this.success = true;
        this.message = "요청성공";
        this.data = data;
    }

    public ApiResponseDto(String message, Object data){
        this.success = true;
        this.message = message;
        this.data = data;
    }

    private ApiResponseDto(String message) {
        this.success = false;
        this.message = message;
        this.data = null;
    }

    public static final ApiResponseDto badRequestUser = new ApiResponseDto("유저가 확인되지 않습니다");
    public static final ApiResponseDto badRequestPlanet = new ApiResponseDto("행성이 확인되지 않습니다");
    public static final ApiResponseDto forbidden = new ApiResponseDto("권한이 없습니다.");
    public static final ApiResponseDto notFound = new ApiResponseDto("잘못된 요청입니다.");
    public static final ApiResponseDto noRefreshToken = new ApiResponseDto("로그아웃 처리 후 로그인 페이지로 이동");
    public static final ApiResponseDto noAccessToken = new ApiResponseDto("엑세스 토큰이 없습니다");

}

