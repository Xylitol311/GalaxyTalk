package com.example.match.controller;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.ApiResponse;
import com.example.match.dto.MatchRequestDto;
import com.example.match.service.MatchService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class MatchController {
    private final MatchService matchService;
    private final Validator validator;

    /**
     * 매칭 시작 요청 처리
     * 1. 요청 데이터 검증
     * 2. UserMatchStatus 객체 생성
     * 3. 매칭 서비스 호출
     */
    @PostMapping("/api/match/start")
    public ResponseEntity<ApiResponse> startMatching(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody MatchRequestDto request) {
        try {
            // MBTI 유효성 검증
            validateMbti(request.getPreferredMbti());

            // UserMatchStatus 객체 생성 및 매칭 시작
            UserMatchStatus status = convertToStatus(request, userId);
            matchService.startMatching(status);

            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "매칭이 시작되었습니다.",
                    null
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Error processing matching request for user {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(
                    false,
                    "매칭 처리 중 오류가 발생했습니다.",
                    null
            ));
        }
    }

    /**
     * 매칭 취소 요청
     */
    @DeleteMapping("/cancel")
    public ResponseEntity<ApiResponse> cancelMatching(
            @RequestHeader("X-User-Id") String userId) {
        try {
            matchService.cancelMatching(userId);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "매칭이 취소되었습니다.",
                    null
            ));
        } catch (Exception e) {
            log.error("Error canceling match for user {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(
                    false,
                    "매칭 취소 중 오류가 발생했습니다.",
                    null
            ));
        }
    }

    /**
     * 매칭 시작 시간 조회
     */
    @GetMapping("/start-time")
    public ResponseEntity<ApiResponse> getMatchingStartTime(
            @RequestHeader("X-User-Id") String userId) {
        try {
            Long startTime = matchService.getMatchingStartTime(userId);
            if (startTime == null) {
                return ResponseEntity.ok(new ApiResponse(
                        false,
                        "매칭 중인 유저가 아닙니다.",
                        null
                ));
            }

            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "매칭 시작 시간 조회 성공",
                    startTime
            ));
        } catch (Exception e) {
            log.error("Error getting start time for user {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponse(
                    false,
                    "매칭 시작 시간 조회 중 오류가 발생했습니다.",
                    null
            ));
        }
    }

    /**
     * 매칭 수락/거절 응답 처리
     * WebSocket을 통한 실시간 응답 처리
     */
    @MessageMapping("/match/response")
    public void handleMatchResponse(MatchResponse response) {
        try {
            validateMatchResponse(response);
            matchService.processMatchResponse(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid match response: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing match response", e);
        }
    }

    /**
     * WebSocket 요청의 유효성 검증
     */
    private void validateMatchResponse(MatchResponse response) {
        Set<ConstraintViolation<MatchResponse>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid match response data");
        }
    }

    /**
     * MBTI 값 유효성 검증
     */
    private void validateMbti(String mbti) {
        try {
            MBTI.valueOf(mbti.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid MBTI value: " + mbti);
        }
    }

    /**
     * 매칭 요청 정보를 UserMatchStatus 객체로 변환
     */
    private UserMatchStatus convertToStatus(MatchRequestDto dto, String userId) {
        UserMatchStatus status = new UserMatchStatus();
        status.setUserId(userId);
        status.setConcern(dto.getConcern());
        status.setPreferredMbti(dto.getPreferredMbti().toUpperCase());
        status.setStatus(MatchStatus.WAITING);
        status.setAccepted(false);
        status.setStartTime(Instant.now().toEpochMilli());
        return status;
    }
}