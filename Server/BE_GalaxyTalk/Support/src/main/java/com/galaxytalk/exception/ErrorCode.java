package com.galaxytalk.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 예시: 피드백 관련 에러 코드 (필요에 따라 추가/수정)
    FEEDBACK_NOT_FOUND("F001", "피드백을 찾을 수 없습니다."),
    ILLEGAL_ARGUMENT("F002", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR("F003", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}

