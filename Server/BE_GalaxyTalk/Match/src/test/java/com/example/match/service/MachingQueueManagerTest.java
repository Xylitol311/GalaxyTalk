package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchingQueueManagerTest {
    @Mock
    private RedisService redisService;
    @Mock
    private MatchingQueueManager matchingQueueManager;

    @BeforeEach
    void setUp() {
        // 직접 초기화할 경우 @Mock을 통해 필요한 의존성을 주입
        matchingQueueManager = new MatchingQueueManager(redisService);
    }

    @Test
    void testAddToQueue() {
        // 예시: 특정 유저를 큐에 추가하는 테스트
        UserMatchStatus user = new UserMatchStatus();
        user.setUserId("user1");
        user.setMbti(MBTI.INFP.name());

        // 큐에서 유저가 추가되는지 확인
        matchingQueueManager.addToQueue(user);

        // 매칭 큐에 대한 동작을 모킹하거나 검증
        Queue<UserMatchStatus> queue = matchingQueueManager.getMbtiQueues().get(MBTI.INFP);
        verify(queue, times(1)).offer(user);
    }

    @Test
    void testGetBatchFromQueue() {
        UserMatchStatus user1 = new UserMatchStatus();
        user1.setUserId("user1");
        user1.setMbti(MBTI.INFP.name());
        user1.setStatus(MatchStatus.WAITING);

        UserMatchStatus user2 = new UserMatchStatus();
        user2.setUserId("user2");
        user2.setMbti(MBTI.INFP.name());
        user2.setStatus(MatchStatus.WAITING);

        matchingQueueManager.addToQueue(user1);
        matchingQueueManager.addToQueue(user2);

        List<UserMatchStatus> batch = matchingQueueManager.getBatchFromQueue(MBTI.INFP);

        // Check if batch contains 2 users
        assertEquals(2, batch.size());
    }

    @Test
    void testMoveToRelatedQueue() {
        UserMatchStatus user = new UserMatchStatus();
        user.setUserId("user1");
        user.setMbti(MBTI.INFP.name());
        user.setPreferredMbti(MBTI.ENTP.name());
        user.setStatus(MatchStatus.WAITING);

        matchingQueueManager.addToQueue(user);
        matchingQueueManager.moveToRelatedQueue(user);

        // Check if the user was moved to the related queue
        assertEquals(1, matchingQueueManager.getQueueSize(MBTI.ENTP));
    }
}
