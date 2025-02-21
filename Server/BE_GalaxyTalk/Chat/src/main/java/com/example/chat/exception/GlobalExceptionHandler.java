package com.example.chat.exception;

import com.example.chat.dto.ApiResponseDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final int CUSTOM_QUESTION_NOT_FOUND_STATUS = 498;
    /**
     * Bean Validation (@Valid) 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "유효하지 않은 입력 값입니다.";
        log.error(errorMessage);
        return ResponseEntity.badRequest()
                .body(new ApiResponseDto(false, errorMessage, null));
    }

    /**
     * @Validated 검사(@NotNull, @Size 등)에서 발생하는 ConstraintViolationException 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDto> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponseDto(false, ex.getMessage(), null));
    }

    /**
     * 단일 커스텀 예외(BusinessException) 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseDto> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        // 에러 코드에 따라 적절한 상태 코드를 맵핑.
        int status;
        // 특정 조건별 상태 코드 매핑
        if (errorCode == ErrorCode.CHAT_ROOM_NOT_FOUND ||
                errorCode == ErrorCode.ACTIVE_CHAT_ROOM_NOT_FOUND ||
                errorCode == ErrorCode.USER_INFO_NOT_FOUND ||
                errorCode == ErrorCode.LETTER_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND.value();
        } else if (errorCode == ErrorCode.INVALID_INPUT) {
            status = HttpStatus.BAD_REQUEST.value();
        } else if (errorCode == ErrorCode.CHAT_ROOM_QUESTION_NOT_FOUND) {
            status = CUSTOM_QUESTION_NOT_FOUND_STATUS;
        } else {
            // LIVEKIT_ROOM_CREATION_FAILED, USER_STATUS_UPDATE_FAILED, GPT_API_FAILED, MESSAGE_SAVE_FAILED 등
            status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        log.error("Business exception occurred: {}, message: {}", errorCode.getCode(), ex.getMessage());
        ApiResponseDto response = new ApiResponseDto(false, ex.getMessage(), null);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 그 외 IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());

        ApiResponseDto response = new ApiResponseDto(false, ex.getMessage(), null);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto> handleException(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto(false, "서버 내부 오류가 발생했습니다.", null));
    }
}