package com.galaxytalk.feedback.controller;

import com.galaxytalk.exception.BusinessException;
import com.galaxytalk.exception.ErrorCode;
import com.galaxytalk.feedback.dto.ApiResponseDto;
import com.galaxytalk.feedback.dto.FeedbackRequestDto;
import com.galaxytalk.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;


    //후기 쓰기
    //user로 가서 에너지 1 늘려주기, requestId는 요청하지 말기
    @Transactional
    @PostMapping
    public ResponseEntity<?> writeLetter(@RequestHeader("X-User-ID") String serialNumber, @RequestBody FeedbackRequestDto requestDto) {
        feedbackService.saveFeedback(serialNumber, requestDto);

        // 작성자 정보 유효성 검증(추가적인 검증이 필요한 경우)
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ILLEGAL_ARGUMENT, "유효한 작성자 정보가 없습니다.");
        }

        ApiResponseDto successResponse = new ApiResponseDto(true, "피드백 저장 성공", null);


        return ResponseEntity.ok(successResponse);

    }

}