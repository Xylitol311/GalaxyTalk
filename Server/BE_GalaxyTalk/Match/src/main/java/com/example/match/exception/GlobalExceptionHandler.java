package com.example.match.exception;

import com.example.match.dto.ApiResponseDto;
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
        HttpStatus status;
        // 특정 조건별 상태 코드 매핑
        if (errorCode == ErrorCode.MATCH_NOT_FOUND ||
                errorCode == ErrorCode.USER_INFO_NOT_FOUND ||
                errorCode == ErrorCode.USER_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else {
            status = HttpStatus.BAD_REQUEST;
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