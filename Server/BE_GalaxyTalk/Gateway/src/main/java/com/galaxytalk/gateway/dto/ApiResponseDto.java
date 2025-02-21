package com.galaxytalk.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import lombok.AllArgsConstructor;
import lombok.Data;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 필드는 JSON에서 제외됨
public class ApiResponseDto {
    private boolean success;
    private String message;
    private Object data;

    // ❌ 기존 static final 제거
    // ✅ 대신 정적 메서드로 객체 생성
    public static ApiResponseDto forbiddenResponse() {
        return new ApiResponseDto(false, "권한이 없습니다.", null);
    }

    public static ApiResponseDto noAccessTokenResponse() {
        return new ApiResponseDto(false, "엑세스 토큰이 없습니다.", null);
    }
}


