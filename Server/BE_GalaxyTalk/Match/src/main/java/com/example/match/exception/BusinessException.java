package com.example.match.exception;

import lombok.Getter;

/**
 * 커스텀 예외 클래스.
 * ErrorCode Enum을 활용해 예외 유형을 식별.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 기본 생성자: ErrorCode만 지정
     * - 기본 메시지는 ErrorCode의 message를 사용.
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 추가 메시지를 함께 사용하는 생성자
     * - ErrorCode의 기본 메시지 + 세부 메시지를 합쳐서 사용.
     */
    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + " - " + detailMessage);
        this.errorCode = errorCode;
    }
}