package com.example.match.controller;

import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchRequestDto;
import com.example.match.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {
    private MockMvc mockMvc;

    @Mock
    private MatchService matchService;

    @Mock
    private Validator validator;

    @InjectMocks
    private MatchController matchController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // GlobalExceptionHandler 제거하고 기본 설정만 사용
        mockMvc = MockMvcBuilders
                .standaloneSetup(matchController)
                .build();
    }

    @Test
    void startMatching_Success() throws Exception {
        // given
        // 매칭 시작을 위한 요청 데이터 준비
        String userId = "user1";
        MatchRequestDto request = new MatchRequestDto();
        request.setPreferredMbti("INFP");
        request.setConcern("테스트 고민이 좀 있습니다.");

        // 서비스 메서드에 전달되는 객체를 캡처하기 위한 설정
        ArgumentCaptor<UserMatchStatus> statusCaptor = ArgumentCaptor.forClass(UserMatchStatus.class);

        // when & then
        mockMvc.perform(post("/api/match/start")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("매칭이 시작되었습니다."));

        // 서비스 메서드 호출 검증
        verify(matchService).startMatching(statusCaptor.capture());

        // 전달된 객체의 값 검증
        UserMatchStatus capturedStatus = statusCaptor.getValue();
        assertAll(
                () -> assertEquals(userId, capturedStatus.getUserId()),
                () -> assertEquals("INFP", capturedStatus.getPreferredMbti()),
                () -> assertEquals("테스트 고민이 좀 있습니다.", capturedStatus.getConcern()),
                () -> assertEquals(MatchStatus.WAITING, capturedStatus.getStatus()),
                () -> assertFalse(capturedStatus.isAccepted())
        );
    }

    @Test
    void cancelMatching_Success() throws Exception {
        // given
        String userId = "user1";

        // when & then
        mockMvc.perform(delete("/cancel")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("매칭이 취소되었습니다."));

        // 서비스 메서드 호출 검증
        verify(matchService).cancelMatching(userId);
    }

    @Test
    void getMatchingStartTime_Success() throws Exception {
        // given
        String userId = "user1";
        long startTime = System.currentTimeMillis();
        when(matchService.getMatchingStartTime(userId)).thenReturn(startTime);

        // when & then
        mockMvc.perform(get("/start-time")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("매칭 시작 시간 조회 성공"))
                .andExpect(jsonPath("$.data").value(startTime));
    }

    @Test
    void getMatchingStartTime_UserNotMatching() throws Exception {
        // given
        String userId = "user1";
        when(matchService.getMatchingStartTime(userId)).thenReturn(null);

        // when & then
        mockMvc.perform(get("/start-time")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("매칭 중인 유저가 아닙니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void handleMatchResponse_Success() throws Exception {
        // given
        // WebSocket을 통한 매칭 응답 데이터 준비
        MatchResponse response = new MatchResponse();
        response.setUserId("user1");
        response.setMatchId("match1");
        response.setAccepted(true);

        // validator가 검증을 통과하도록 설정
        when(validator.validate(any(MatchResponse.class))).thenReturn(Collections.emptySet());

        // when
        matchController.handleMatchResponse(response);

        // then
        // 서비스 메서드 호출 검증
        verify(matchService).processMatchResponse(response);
    }

    @Test
    void handleMatchResponse_ValidationFailed() throws Exception {
        // given
        MatchResponse response = new MatchResponse();
        // validator가 검증 실패를 반환하도록 설정
        Set<ConstraintViolation<MatchResponse>> violations = new HashSet<>();
        violations.add(mock(ConstraintViolation.class));
        when(validator.validate(any(MatchResponse.class))).thenReturn(violations);

        // when
        matchController.handleMatchResponse(response);

        // then
        // 검증 실패로 인해 서비스 메서드가 호출되지 않아야 함
        verify(matchService, never()).processMatchResponse(any());
    }
}