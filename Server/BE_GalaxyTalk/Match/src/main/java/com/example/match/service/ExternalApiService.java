package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResultRequest;
import com.example.match.dto.SimilarityResponseDto;
import com.example.match.feign.AuthClient;
import com.example.match.feign.ChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiService {
    private final WebClient aiServiceClientWithoutLoadBalancing;
//    private final WebClient chatServiceClient;
//    private final WebClient authServiceClient;
    private final AuthClient authServiceClient;
    private final ChatClient chatServiceClient;

//    public UserResponseDto.UserSendDTO getUserInfo(String userId) {
//        log.info("Getting user info from Auth Service...");
//        return authServiceClient.get()
//                .uri("/api/oauth")
//                .header("X-User-ID", userId)
//                .retrieve()
//                .bodyToMono(UserResponseDto.class) // 1차적으로 ApiResponseDto로 변환
//                .map(UserResponseDto::getData)
//                .block();
//    }

    /**
     * AI 서버에 두 유저 간의 유사도 점수 계산 요청
     */
    public double calculateSimilarity(UserMatchStatus user1, UserMatchStatus user2) {
        log.info("calculating similarity 실행");
        Map<String, Object> request = Map.of(
                "sentence1", user1.getConcern(),
                "sentence2", user2.getConcern()
        );

        return aiServiceClientWithoutLoadBalancing.post()
                .uri("/calculate-similarity")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SimilarityResponseDto.class)
                .map(SimilarityResponseDto::getSimilarityScore)
                .defaultIfEmpty(0.0)
                .block();
    }

    /**
     * 채팅방 생성 요청
     * - Match 서버에서 유저 아이디, 고민 내용, 유사도 점수를 Chat 서버로 전달
     * - Chat 서버에서 sessionId, token 및 chatRoomId 반환
     */
//    public ChatRoomResponseDto.ChatResponse createChatRoom(UserMatchStatus user1, UserMatchStatus user2, double similarityScore) {
//        log.info("Createing chat room from Chat Service...");
//        Map<String, Object> requestBody = Map.of(
//                "userId1", user1.getUserId(),
//                "userId2", user2.getUserId(),
//                "concern1", user1.getConcern(),
//                "concern2", user2.getConcern(),
//                "similarityScore", similarityScore
//        );
//
//        return chatServiceClient.post()
//                .uri("/api/chat/match")
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(ChatRoomResponseDto.class) // Chat 서버 응답을 ChatRoomResponseDto로 변환
//                .map(ChatRoomResponseDto::getData) // data 필드(ChatResponse)만 추출
//                .block();
//    }
    public Map<String, Object> createChatRoom(UserMatchStatus user1, UserMatchStatus user2, double similarityScore) {
        log.info("Createing chat room from Chat Service...");


        MatchResultRequest matchResultRequest = new MatchResultRequest();
        matchResultRequest.setConcern1(user1.getConcern());
        matchResultRequest.setConcern2(user2.getConcern());
        matchResultRequest.setUserId1(user1.getUserId());
        matchResultRequest.setUserId2(user2.getUserId());
        matchResultRequest.setSimilarityScore(similarityScore);
        Map<String, Object> response = (Map<String, Object>) chatServiceClient.createSessionWithTwoTokens(matchResultRequest).getBody();
        return (Map<String, Object>) response.get("data");
    }

    /**
     * 세션 서버에 유저 상태 변경 요청 (JSON 형식)
     */
//    public void setUserStatus(String userId, String status) {
//        log.info("Setting user status to " + status);
//        authServiceClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/api/oauth/status")
//                        .queryParam("userInteractionState", status)
//                        .build()
//                )
//                .header("X-User-ID", userId)
//                .retrieve() // 2xx 응답이면 정상 처리, 4xx/5xx이면 예외 발생
//                .bodyToMono(Void.class) // 응답 본문을 무시
//                .block();
//    }
    // Auth server에 유저 정보가 idle 또는 chatting으로 변경됨을 알림
    public void setUserStatus(String userId, String status) {

        System.out.println(userId + " " + status);
        authServiceClient.changeUserStatus(
                userId,
                status
        );
    }

    // Auth server에서 상대방 id를 가지고 상대방 정보를 가져옴
    // 행성 ID, MBTI, 매너온도 데이터 추출
    public Map<String, Object> getUserInfo(String userId) {
        Map<String, Object> response = (Map<String, Object>) authServiceClient.getUser(userId).getBody();
        return (Map<String, Object>) response.get("data");
    }

}
