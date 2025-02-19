package com.example.match.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스에서 발생할 수 있는 에러 코드 목록을 정의
 * - code: 에러 식별 코드(HttpStatus 외에 세부적으로 에러 사항을 파악하기 위해 사용)
 * - message: 기본 에러 메시지
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 매칭 관련 에러 코드
    INVALID_MBTI("M001", "Invalid MBTI value"),
    MATCH_NOT_FOUND("M002", "매칭 중인 유저가 아닙니다."),
    MATCH_ALREADY_IN_PROGRESS("M003", "이미 매칭 중인 유저입니다."),
    USER_NOT_MATCHING("M004", "매칭 중인 유저가 아닙니다."),

    // 사용자 정보 관련 에러 코드
    USER_INFO_NOT_FOUND("U001", "유저 정보를 찾을 수 없습니다."),
    USER_NOT_FOUND("U002", "해당 유저 정보를 찾을 수 없습니다."),

    // Redis 관련 (새로 추가 예시)
    REDIS_DATA_MISMATCH("R001", "Redis에 저장된 데이터 형식이 올바르지 않습니다."),

    // 공통 에러 코드
    ILLEGAL_ARGUMENT("C001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR("C002", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
