package com.example.match.controller;

import com.example.match.constant.MBTI;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.*;
import com.example.match.exception.BusinessException;
import com.example.match.exception.ErrorCode;
import com.example.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match")
public class MatchController {
    private final MatchService matchService;

    /**
     * 매칭 시작 요청 처리
     * 1. 요청 데이터 검증
     * 2. UserMatchStatus 객체 생성
     * 3. 매칭 서비스 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto> startMatching(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody MatchRequestDto request) {
        log.info("Start matching request");

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
    }

    /**
     * 매칭 취소 요청
     */
    @DeleteMapping
    public ResponseEntity<ApiResponseDto> cancelMatching(
            @RequestHeader("X-User-ID") String userId) {
        log.info("Cancel matching request");
        matchService.cancelMatching(userId);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "매칭이 취소되었습니다.",
                null
        ));
    }

    /**
     * 매칭 대기 중인 유저 목록 조회
     * - 실시간으로 매칭 대기 유저를 클라이언트 화면에 표시하기 위한 기능
     */
    @GetMapping("/waiting-users")
    public ResponseEntity<ApiResponseDto> getWaitingUsers() {
        log.info("getWaitingUsers");
        List<UserStatusDto> userStatusDtos = matchService.getWaitingUsers();
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "매칭 대기 중인 유저 조회 성공",
                userStatusDtos
        ));
    }

    /**
     * 매칭 시작 시간 조회
     */
    @GetMapping("/start-time")
    public ResponseEntity<ApiResponseDto> getMatchingStartTime(
            @RequestHeader("X-User-ID") String userId) {
        log.info("getMatchingStartTime");
        Long startTime = matchService.getMatchingStartTime(userId);
        if (startTime == null) {
            // 매칭 중인 사용자가 아니라면 예외 발생
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        }

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "매칭 시작 시간 조회 성공",
                startTime
        ));
    }


    /**
     * 매칭 수락/거절 응답 처리
     */
    @PostMapping("/approve")
    public ResponseEntity<ApiResponseDto> handleMatchResponse(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody MatchApproveRequestDto response) {
        log.info("handleMatchResponse");
        matchService.processMatchApproval(userId, response);

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                response.isAccepted() ? "매칭을 수락했습니다." : "매칭을 거절했습니다.",
                null));
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