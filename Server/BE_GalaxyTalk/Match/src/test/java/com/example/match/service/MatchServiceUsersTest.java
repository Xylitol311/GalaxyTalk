package com.example.match.service;

import com.example.match.constant.MBTI;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@SpringBootTest
class MatchServiceUsersTest {
    @Mock
    private RedisService redisService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private MatchingQueueManager queueManager;
    @Mock
    private MatchProcessor matchProcessor;
    @Mock
    private ExternalApiService mockAuthService; // Auth 관련 메소드만 Mock으로 처리

    @Autowired
    private ExternalApiService realExternalApiService; // 실제 AI 서버 통신용

    private MatchService matchService;

    @Captor
    private ArgumentCaptor<UserMatchStatus> userStatusCaptor;

    private final String[] CONCERNS = {
            // 건강 관련 고민
            "살이 자꾸 찌는데 다이어트 어떻게 시작해야 할지 모르겠어요.",
            "불면증이 심해서 매일 밤 잠들기가 힘들어요.",
            "머리가 자주 아픈데 병원에 가기는 귀찮고 걱정되네요.",
            "운동을 시작하고 싶은데 어떤 운동부터 시작해야 할지 모르겠어요.",
            "자세가 너무 안 좋은데 교정하는 방법 추천해주세요.",

            // 연애/인간관계 고민
            "짝사랑하는 사람한테 고백하고 싶은데 용기가 안 나요.",
            "애인이랑 취미가 너무 달라서 데이트 코스 정하기가 힘들어요.",
            "친구랑 다툰 후에 먼저 연락하기가 망설여져요.",
            "소개팅 나가기로 했는데 뭘 입어야 할지 고민이에요.",
            "연인이 갑자기 연락이 뜸한데 어떻게 해야 할까요.",

            // 취업/진로 고민
            "취업 준비 중인데 스펙이 너무 부족한 것 같아요.",
            "이직을 하고 싶은데 연봉 협상은 어떻게 해야 할까요.",
            "공무원 시험 준비할지 일반 기업 취업할지 고민이에요.",
            "면접 볼 때 자주 긴장하는데 극복하는 방법 있을까요.",
            "현재 회사에서 업무가 너무 과중한데 어떻게 해결해야 할까요.",

            // 일상적인 소소한 고민
            "오늘 점심 메뉴 추천해주세요. 매일 같은 거만 먹어요.",
            "주말에 뭐하고 놀지 추천해주세요.",
            "집안 정리정돈 하는 게 너무 귀찮아요.",
            "요즘 넷플릭스에서 볼만한 드라마 추천해주세요.",
            "반려동물 처음 키우는데 어떤 걸 준비해야 할까요.",

            // 금전적 고민
            "적금 들고 싶은데 어떤 상품이 좋을까요.",
            "갑자기 큰 지출이 생겼는데 돈을 어떻게 마련해야 할지 모르겠어요.",
            "월급 받자마자 다 쓰는 습관을 고치고 싶어요.",
            "투자를 시작하고 싶은데 어디서부터 시작해야 할지 모르겠어요.",
            "생활비 절약하는 꿀팁 알려주세요.",

            // 가족 관련 고민
            "부모님이 퇴직하셨는데 용돈을 얼마나 드려야 할지 모르겠어요.",
            "동생이랑 자주 다투는데 어떻게 하면 사이가 좋아질까요.",
            "결혼 후 시댁과의 관계가 어려워요.",
            "부모님의 건강이 걱정되는데 어떻게 챙겨드려야 할까요.",
            "집에서 혼자 있는 시간이 너무 없어요.",

            // 취미/여가 고민
            "취미를 찾고 싶은데 뭐가 좋을지 모르겠어요.",
            "여행 가고 싶은데 혼자 가기 무서워요.",
            "악기를 배우고 싶은데 너무 늦은 나이는 아닐까요.",
            "사진 찍는 게 좋은데 카메라 추천해주세요.",
            "등산 입문자인데 어떤 코스부터 시작할까요.",

            // 심리/정신건강 고민
            "스트레스 해소법 좀 알려주세요.",
            "무기력증이 심한데 어떻게 극복하나요.",
            "우울할 때 혼자서 할 수 있는 것들 추천해주세요.",
            "긍정적으로 생각하는 연습을 하고 싶어요.",
            "완벽주의 성향 때문에 스트레스 받아요."
    };

    @BeforeEach
    void setUp() {
        // AI 서버용 WebClient 생성
        WebClient aiServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8000")  // AI 서버의 실제 주소로 변경 필요
                .build();

        // 실제 AI 서버와 통신할 ExternalApiService 생성
        realExternalApiService = new ExternalApiService(
                aiServiceClient,
                null,  // chatServiceClient
                null   // authServiceClient
        );

        matchService = new MatchService(
                redisService,
                webSocketService,
                mockAuthService,  // Auth 관련 메서드는 Mock 사용
                queueManager,
                matchProcessor
        );

        // 실제 AI 서버와 통신하는 calculateSimilarity는 realExternalApiService 사용
        when(mockAuthService.calculateSimilarity(any(), any())).thenAnswer(invocation -> {
            UserMatchStatus user1 = invocation.getArgument(0);
            UserMatchStatus user2 = invocation.getArgument(1);
            return realExternalApiService.calculateSimilarity(user1, user2);
        });

        matchService = new MatchService(
                redisService,
                webSocketService,
                mockAuthService, // Mock된 Auth 서비스 주입
                queueManager,
                matchProcessor
        );
    }

    @Test
    void processMatching_With100Users() {
        // given
        List<UserMatchStatus> users = createTestUsers(100);
        long startTime = System.currentTimeMillis();
        Map<String, Integer> matchedPairs = new HashMap<>();

        // Mock RedisService behavior
        when(redisService.getUserStatus(anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return users.stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        });

        // Mock QueueManager behavior
        when(queueManager.getBatchFromQueue(any(MBTI.class))).thenAnswer(invocation -> {
            MBTI mbti = invocation.getArgument(0);
            return users.stream()
                    .filter(u -> u.getMbti().equals(mbti.name()))
                    .filter(u -> u.getStatus() == MatchStatus.WAITING)
                    .limit(50)
                    .collect(Collectors.toList());
        });

        // when
        List<CompletableFuture<Void>> futures = Arrays.stream(MBTI.values())
                .map(mbti -> CompletableFuture.runAsync(() -> {
                    try {
                        matchService.processMbtiQueue(mbti);
                    } catch (Exception e) {
                        fail("Error processing queue: " + e.getMessage());
                    }
                }))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

//        verify(matchProcessor, atLeastOnce()).createMatch(userStatusCaptor.capture(), userStatusCaptor.capture());
        List<UserMatchStatus> matchedUsers = userStatusCaptor.getAllValues();

        System.out.println("\nMatching Results:");
        System.out.println("Total time: " + duration + "ms");
        System.out.println("Number of matches: " + matchedUsers.size() / 2);

        // Print detailed match information
        for (int i = 0; i < matchedUsers.size(); i += 2) {
            UserMatchStatus user1 = matchedUsers.get(i);
            UserMatchStatus user2 = matchedUsers.get(i + 1);

            double similarity = realExternalApiService.calculateSimilarity(user1, user2);

            System.out.println("\nMatch #" + (i/2 + 1) + ":");
            System.out.println("User " + user1.getUserId() + " (" + user1.getMbti() +
                    ") ↔ User " + user2.getUserId() + " (" + user2.getMbti() + ")");
            System.out.println("Similarity Score: " + String.format("%.2f", similarity));

            String matchKey = user1.getMbti() + "-" + user2.getMbti();
            matchedPairs.merge(matchKey, 1, Integer::sum);
        }

        // Print MBTI pair statistics
        System.out.println("\nMBTI Pair Statistics:");
        matchedPairs.forEach((pair, count) ->
                System.out.println(pair + ": " + count + " matches"));

        assertTrue(duration < 30000, "Matching should complete within 30 seconds");
        assertTrue(matchedUsers.size() >= 40, "At least 80 users should be matched");
    }

    @Test
    void testMatchingBetweenSimilarMbtis() {
        // given
        UserMatchStatus user1 = new UserMatchStatus();
        user1.setUserId("user1");
        user1.setMbti("INFP");
        user1.setStatus(MatchStatus.WAITING);
        user1.setConcern("취미를 찾고 싶은데 뭐가 좋을지 모르겠어요.");
        user1.setStartTime(System.currentTimeMillis());

        UserMatchStatus user2 = new UserMatchStatus();
        user2.setUserId("user2");
        user2.setMbti("INFJ");
        user2.setStatus(MatchStatus.WAITING);
        user2.setConcern("새로운 취미를 시작하고 싶은데 어떤 걸 해야될지 고민이에요.");
        user2.setStartTime(System.currentTimeMillis());

        List<UserMatchStatus> users = Arrays.asList(user1, user2);

        // Mock RedisService behavior
        when(redisService.getUserStatus(anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return users.stream()
                    .filter(u -> u.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        });

        // Mock QueueManager behavior for both queues
        when(queueManager.getBatchFromQueue(any(MBTI.class))).thenAnswer(invocation -> {
            MBTI mbti = invocation.getArgument(0);
            if (mbti == MBTI.INFP || mbti == MBTI.INFJ) {
                return users;
            }
            return Collections.emptyList();
        });

        // when
        matchService.processMbtiQueue(MBTI.INFP);

        // then
//        verify(matchProcessor, timeout(5000).atLeast(1)).createMatch(userStatusCaptor.capture(), userStatusCaptor.capture());
        List<UserMatchStatus> matchedUsers = userStatusCaptor.getAllValues();

        if (!matchedUsers.isEmpty()) {
            assertEquals(2, matchedUsers.size(), "Should have exactly two matched users");

            UserMatchStatus matchedUser1 = matchedUsers.get(0);
            UserMatchStatus matchedUser2 = matchedUsers.get(1);

            System.out.println("\nMatching Result:");
            System.out.println("User " + matchedUser1.getUserId() + " (" + matchedUser1.getMbti() +
                    ") ↔ User " + matchedUser2.getUserId() + " (" + matchedUser2.getMbti() + ")");
            System.out.println("User 1 concern: " + matchedUser1.getConcern());
            System.out.println("User 2 concern: " + matchedUser2.getConcern());

            double similarity = realExternalApiService.calculateSimilarity(matchedUser1, matchedUser2);
            System.out.println("Similarity Score: " + String.format("%.2f", similarity));

        } else {
            System.out.println("\nNo matching occurred between INFP and INFJ users.");
            System.out.println("This might indicate that the similarity threshold was not met.");
            fail("Expected matching between INFP and INFJ users but no matching occurred");
        }
    }

    private List<UserMatchStatus> createTestUsers(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    UserMatchStatus user = new UserMatchStatus();
                    user.setUserId("user" + i);
                    user.setMbti(getRandomMbti());
                    user.setStatus(MatchStatus.WAITING);
                    user.setConcern(CONCERNS[i % CONCERNS.length]);
                    user.setStartTime(System.currentTimeMillis());
                    return user;
                })
                .collect(Collectors.toList());
    }

    private String getRandomMbti() {
        List<String> types = Arrays.stream(MBTI.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return types.get(new Random().nextInt(types.size()));
    }
}