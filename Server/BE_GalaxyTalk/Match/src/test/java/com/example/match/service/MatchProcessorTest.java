package com.example.match.service;

import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchProcessorTest {
    @Mock
    private RedisService redisService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private ExternalApiService externalApiService;
    @Mock
    private MatchingQueueManager queueManager;

    @InjectMocks
    private MatchProcessor matchProcessor;

    // userIds 리스트를 캡처하기 위한 Captor
    @Captor
    private ArgumentCaptor<List<String>> userIdsCaptor;

    /**
     * 매칭 생성 테스트
     */
    @Test
    void createMatch_Success() {
        // given
        UserMatchStatus user1 = createTestUser("user1");
        UserMatchStatus user2 = createTestUser("user2");

        // when
        matchProcessor.createMatch(user1, user2);

        // then
        verify(redisService, times(2)).saveUserStatus(any(UserMatchStatus.class));
        verify(redisService).saveMatchInfo(anyString(), userIdsCaptor.capture());
        verify(webSocketService).notifyMatch(eq("user1"), eq("user2"), anyString());

        List<String> savedUserIds = userIdsCaptor.getValue();
        assertEquals(2, savedUserIds.size());
        assertTrue(savedUserIds.containsAll(List.of("user1", "user2")));
    }

    /**
     * 매칭 수락 처리 테스트 - 양쪽 모두 수락한 경우
     */
    @Test
    void processAcceptance_BothAccepted() {
        // given: 두 유저가 모두 수락한 상황 설정
        String matchId = "match1";
        // 테스트용 유저 객체들 생성
        UserMatchStatus user1 = createTestUser("user1");
        UserMatchStatus user2 = createTestUser("user2");
        // 매칭 ID 설정
        user1.setMatchId(matchId);
        user2.setMatchId(matchId);
        // user2는 이미 수락한 상태
        user2.setAccepted(true);

        // Redis에서 매칭 정보와 유저 정보를 조회할 때 반환할 값 설정
        List<String> userIds = List.of("user1", "user2");
        when(redisService.getMatchInfo(matchId)).thenReturn(userIds);
        when(redisService.getUserStatus("user1")).thenReturn(user1);
        when(redisService.getUserStatus("user2")).thenReturn(user2);

        // when: user1이 수락하는 상황 실행
//        matchProcessor.processAcceptance(user1);

        // then: 양쪽이 모두 수락했으므로 채팅방 생성과 정리 작업이 실행되어야 함
        // 정확한 userIds로 채팅방이 생성되었는지 검증
        verify(externalApiService).createChatRoom(userIds);
        // 두 유저의 정보가 Redis에서 삭제되었는지 검증
        verify(redisService).deleteUserStatus("user1");
        verify(redisService).deleteUserStatus("user2");
        // 매칭 정보가 Redis에서 삭제되었는지 검증
        verify(redisService).deleteMatchInfo(matchId);
    }

    /**
     * 매칭 거절 처리 테스트
     */
    @Test
    void processRejection_Success() {
        // given
        String matchId = "match1";
        UserMatchStatus user1 = createTestUser("user1");
        UserMatchStatus user2 = createTestUser("user2");
        user1.setMatchId(matchId);
        user2.setMatchId(matchId);

        List<String> userIds = List.of("user1", "user2");
        when(redisService.getMatchInfo(matchId)).thenReturn(userIds);
        when(redisService.getUserStatus("user2")).thenReturn(user2);

        // when
//        matchProcessor.processRejection(user1);

        // then
        verify(redisService, times(2)).saveUserStatus(any(UserMatchStatus.class));
        verify(queueManager, times(2)).addToQueue(any(UserMatchStatus.class));
        verify(webSocketService).notifyUser(eq("user2"), eq("MATCH_FAILED"), anyString());
    }

    /**
     * 매칭 응답 처리 테스트 - 유저를 찾을 수 없는 경우
     */
    @Test
    void processMatchResponse_UserNotFound() {
        // given
        MatchResponse response = new MatchResponse();
        response.setUserId("user1");
        response.setAccepted(true);

        when(redisService.getUserStatus("user1")).thenReturn(null);

        // when
        matchProcessor.processMatchResponse(response);

        // then
        verify(externalApiService, never()).createChatRoom(any());
        verify(queueManager, never()).addToQueue(any());
    }

    private UserMatchStatus createTestUser(String userId) {
        UserMatchStatus user = new UserMatchStatus();
        user.setUserId(userId);
        user.setStatus(MatchStatus.MATCHED);
        user.setConcern("테스트 고민");
        return user;
    }
}
