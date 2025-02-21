package com.example.chat.exception;

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

    // LiveKit 관련 오류
    LIVEKIT_ROOM_CREATION_FAILED("L001", "LiveKit 방 생성에 실패했습니다."),

    // 채팅방 관련 오류
    CHAT_ROOM_NOT_FOUND("C002", "채팅방을 찾을 수 없습니다."),
    ACTIVE_CHAT_ROOM_NOT_FOUND("C003", "활성 채팅방이 존재하지 않습니다."),
    CHAT_ROOM_QUESTION_NOT_FOUND("C004", "채팅방의 질문이 아직 생성되지 않았습니다."),
    CHAT_ROOM_SAVE_FAILED("C005", "채팅방 저장에 실패했습니다."),

    // 사용자 관련 오류
    USER_STATUS_UPDATE_FAILED("U001", "사용자 상태 업데이트에 실패했습니다."),
    USER_INFO_NOT_FOUND("U002", "유저 정보를 찾을 수 없습니다."),
    LETTER_NOT_FOUND("L002", "후기 정보를 찾을 수 없습니다."),

    // GPT 관련 오류
    GPT_API_FAILED("G001", "GPT API 호출에 실패했습니다."),

    // 메시지 저장 관련 오류
    MESSAGE_SAVE_FAILED("M003", "메시지 저장에 실패했습니다."),

    // 입력값 오류
    INVALID_INPUT("C005", "잘못된 요청입니다.");

    private final String code;
    private final String message;
}
