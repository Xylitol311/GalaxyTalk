package com.galaxytalk.auth.exception;

import com.galaxytalk.auth.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto> handleAllExceptions(Exception ex) {
        // log 프레임워크를 사용하여 자세한 오류 로그 남기기
        log.error("Unhandled exception caught: ", ex);
        ApiResponseDto response = new ApiResponseDto("서버 내부 오류가 발생했습니다.", null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
