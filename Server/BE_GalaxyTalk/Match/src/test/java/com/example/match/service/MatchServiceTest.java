package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // JUnit5에서 Mockito를 사용하기 위한 확장 설정
class MatchServiceTest {
    // 실제 객체 대신 가짜(Mock) 객체를 주입받습니다.
    @Mock
    private RedisService redisService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private ExternalApiService externalApiService;
    @Mock
    private MatchingQueueManager queueManager;
    @Mock
    private MatchProcessor matchProcessor;

    // 위에서 생성한 Mock 객체들을 자동으로 주입받는 실제 테스트 대상 객체입니다.
    @InjectMocks
    private MatchService matchService;

    // Mock 객체의 메서드 호출 시 전달된 파라미터를 캡처하여 검증하는데 사용됩니다.
    @Captor
    private ArgumentCaptor<UserMatchStatus> userCaptor;

    @Test
    void startMatching_Success() {
        // given: 테스트를 위한 초기 조건 설정
        // 테스트용 유저 객체 생성
        UserMatchStatus user = createTestUser("user1");
        // MBTI 정보를 담은 응답 객체 생성
        UserResponseDto userResponse = new UserResponseDto();
        userResponse.setMbti("INFP");

        // externalApiService.getUserInfo() 호출 시 미리 준비한 userResponse를 반환하도록 설정
        when(externalApiService.getUserInfo("user1"))
                .thenReturn(userResponse);

        // when: 테스트할 메서드 실행
        matchService.startMatching(user);

        // then: 결과 검증
        // redisService.saveUserStatus()가 호출되었는지 확인하고, 전달된 파라미터를 캡처
        verify(redisService).saveUserStatus(userCaptor.capture());
        // queueManager.addToQueue()가 호출되었는지 확인하고, 캡처된 파라미터와 동일한 값이 전달되었는지 검증
        verify(queueManager).addToQueue(userCaptor.getValue());
        // webSocketService.notifyUser()가 정확한 파라미터로 호출되었는지 검증
        // eq()는 정확한 값 비교, anyString()은 어떤 문자열이든 허용
        verify(webSocketService).notifyUser(eq("user1"), eq("WAITING"), anyString());
        verify(webSocketService).broadcastNewUser(userCaptor.getValue());

        // 캡처된 UserMatchStatus 객체의 상태 검증
        UserMatchStatus savedUser = userCaptor.getValue();
        assertAll(
                () -> assertEquals("INFP", savedUser.getMbti()),  // MBTI 값이 정확히 설정되었는지
                () -> assertEquals(MatchStatus.WAITING, savedUser.getStatus()),  // 상태가 WAITING으로 설정되었는지
                () -> assertNotNull(savedUser.getStartTime())  // 시작 시간이 설정되었는지
        );
    }

    @Test
    void startMatching_WithoutMbti() {
        // given: MBTI 정보가 없는 상황 설정
        UserMatchStatus user = createTestUser("user1");
        // getUserInfo() 호출 시 null 반환하도록 설정
        when(externalApiService.getUserInfo("user1")).thenReturn(null);

        // when: 매칭 시작 메서드 실행
        matchService.startMatching(user);

        // then: MBTI 정보가 없으므로 어떤 후속 작업도 실행되지 않아야 함
        // never()를 사용하여 메서드가 호출되지 않았음을 검증
        verify(redisService, never()).saveUserStatus(any());
        verify(queueManager, never()).addToQueue(any());
        verify(webSocketService, never()).notifyUser(anyString(), anyString(), anyString());
    }


    /**
     * 매칭 취소 테스트
     */
    @Test
    void cancelMatching_Success() {
        // given
        String userId = "user1";

        // when
        matchService.cancelMatching(userId);

        // then
        verify(redisService).deleteUserStatus(userId);
        verify(webSocketService).broadcastUserExit(userId);
    }

    /**
     * 매칭 시작 시간 조회 테스트
     */
    @Test
    void getMatchingStartTime_Success() {
        // given
        String userId = "user1";
        UserMatchStatus user = createTestUser(userId);
        user.setStartTime(System.currentTimeMillis());

        when(redisService.getUserStatus(userId)).thenReturn(user);

        // when
        Long startTime = matchService.getMatchingStartTime(userId);

        // then
        assertNotNull(startTime);
        assertEquals(user.getStartTime(), startTime);
    }

    /**
     * 매칭 큐 처리 테스트 - 배치 크기가 1인 경우
     */
    @Test
    void processMbtiQueue_SingleUser() {
        // given
        MBTI mbti = MBTI.INFP;
        UserMatchStatus user = createTestUser("user1");
        when(queueManager.getBatchFromQueue(mbti))
                .thenReturn(List.of(user));

        // when
        CompletableFuture<Void> future = matchService.processMbtiQueue(mbti);

        // then
        verify(queueManager).moveToRelatedQueue(user);
        assertNotNull(future);
    }

    /**
     * 매칭 프로세스 테스트 - 유사도 기준 충족하는 경우
     */
    @Test
    void processMatching_Success() {
        // given
        UserMatchStatus user1 = createTestUser("user1");
        UserMatchStatus user2 = createTestUser("user2");
        List<UserMatchStatus> users = List.of(user1, user2);

        when(redisService.getUserStatus(user1.getUserId())).thenReturn(user1);
        when(redisService.getUserStatus(user2.getUserId())).thenReturn(user2);
        when(externalApiService.calculateSimilarity(user1, user2)).thenReturn(0.8);

        // when
//        matchService.processMatching(users);

        // then
        verify(matchProcessor).createMatch(user1, user2);
    }

    private UserMatchStatus createTestUser(String userId) {
        UserMatchStatus user = new UserMatchStatus();
        user.setUserId(userId);
        user.setStatus(MatchStatus.WAITING);
        user.setConcern("테스트 고민");
        return user;
    }
}