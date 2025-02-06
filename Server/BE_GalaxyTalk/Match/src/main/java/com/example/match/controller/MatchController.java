package com.example.match.controller;

import com.example.match.constant.MBTI;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.ApiResponseDto;
import com.example.match.dto.MatchRequestDto;
import com.example.match.dto.MatchResponseRequestDto;
import com.example.match.dto.UserStatusDto;
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

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/match")
public class MatchController {
    private final MatchService matchService;
    private final Validator validator;

    /**
     * 매칭 시작 요청 처리
     * 1. 요청 데이터 검증
     * 2. UserMatchStatus 객체 생성
     * 3. 매칭 서비스 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto> startMatching(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody MatchRequestDto request) {
        try {
            // MBTI 유효성 검증
            if (request.getPreferredMbti() != null) {
                validateMbti(request.getPreferredMbti());
            }

            // UserMatchStatus 객체 생성 및 매칭 시작
            UserMatchStatus status = convertToStatus(request, userId);
            matchService.startMatching(status);

            return ResponseEntity.ok(new ApiResponseDto(
                    true,
                    "매칭이 시작되었습니다.",
                    null
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDto(
                    false,
                    e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Error processing matching request for user {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponseDto(
                    false,
                    "매칭 처리 중 오류가 발생했습니다.",
                    null
            ));
        }
    }

    /**
     * 매칭 취소 요청
     */
    @DeleteMapping
    public ResponseEntity<ApiResponseDto> cancelMatching(
            @RequestHeader("X-User-Id") String userId) {
        try {
            matchService.cancelMatching(userId);
            return ResponseEntity.ok(new ApiResponseDto(
                    true,
                    "매칭이 취소되었습니다.",
                    null
            ));
        } catch (Exception e) {
            log.error("Error canceling match for user {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponseDto(
                    false,
                    "매칭 취소 중 오류가 발생했습니다.",
                    null
            ));
        }
    }


    @GetMapping("/user-status")
    public ResponseEntity<ApiResponseDto> getWaitingUser(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            matchService.getWaitingUser(userId);

            return ResponseEntity.ok(new ApiResponseDto(
                    true,
                    "매칭 대기 중인 유저 조회 성공",
                    null
            ));
        } catch (Exception e) {
            log.error("Error getting user information waiting for matching {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponseDto(
                    false,
                    "매칭 시작 시간 조회 중 오류가 발생했습니다.",
                    null
            ));
        }
    }

    /**
     * 매칭 대기 중인 유저 목록 조회
     * - 실시간으로 매칭 대기 유저를 클라이언트 화면에 표시하기 위한 기능
     */
    @GetMapping("/waiting-users")
    public ResponseEntity<ApiResponseDto> getWaitingUsers(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            List<UserStatusDto> userStatusDtos = matchService.getWaitingUsers();

            return ResponseEntity.ok(new ApiResponseDto(
                    true,
                    "매칭 대기 중인 유저 조회 성공",
                    userStatusDtos
            ));
        } catch (Exception e) {
            log.error("Error getting user information waiting for matching {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponseDto(
                    false,
                    "매칭 시작 시간 조회 중 오류가 발생했습니다.",
                    null
            ));
        }
    }

    /**
     * 매칭 시작 시간 조회
     */
    @GetMapping("/start-time")
    public ResponseEntity<ApiResponseDto> getMatchingStartTime(
            @RequestHeader("X-User-Id") String userId) {
        try {
            Long startTime = matchService.getMatchingStartTime(userId);
            if (startTime == null) {
                return ResponseEntity.ok(new ApiResponseDto(
                        false,
                        "매칭 중인 유저가 아닙니다.",
                        startTime
                ));
            }

            return ResponseEntity.ok(new ApiResponseDto(
                    true,
                    "매칭 시작 시간 조회 성공",
                    startTime
            ));
        } catch (Exception e) {
            log.error("Error getting start time for user {}", userId, e);
            return ResponseEntity.internalServerError().body(new ApiResponseDto(
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
    public void handleMatchResponse(MatchResponseRequestDto response) {
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
    private void validateMatchResponse(MatchResponseRequestDto response) {
        Set<ConstraintViolation<MatchResponseRequestDto>> violations = validator.validate(response);
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
        if (dto.getPreferredMbti() != null) {
            status.setPreferredMbti(dto.getPreferredMbti().toUpperCase());
        }
        return status;
    }
}