package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.ChatRoomResponseDto;
import com.example.match.dto.SimilarityResponseDto;
import com.example.match.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalApiService {
    private final WebClient aiServiceClientWithoutLoadBalancing;
    private final WebClient chatServiceClient;
    private final WebClient authServiceClient;

    public UserResponseDto.UserSendDTO getUserInfo(String userId) {
        return authServiceClient.get()
                .uri("/api/oauth?userId=" + userId)
                .header("X-User-ID", userId)
                .retrieve()
                .bodyToMono(UserResponseDto.class) // 1차적으로 ApiResponseDto로 변환
                .map(UserResponseDto::getData)
                .block();
    }

    /**
     * AI 서버에 두 유저 간의 유사도 점수 계산 요청
     */
    public double calculateSimilarity(UserMatchStatus user1, UserMatchStatus user2) {
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
    public ChatRoomResponseDto.ChatResponse createChatRoom(UserMatchStatus user1, UserMatchStatus user2, double similarityScore) {
        Map<String, Object> requestBody = Map.of(
                "userId1", user1.getUserId(),
                "userId2", user2.getUserId(),
                "concern1", user1.getConcern(),
                "concern2", user2.getConcern(),
                "similarityScore", similarityScore
        );

        return chatServiceClient.post()
                .uri("/api/chat/match")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatRoomResponseDto.class) // Chat 서버 응답을 ChatRoomResponseDto로 변환
                .map(ChatRoomResponseDto::getData) // data 필드(ChatResponse)만 추출
                .block();
    }

    /**
     * 세션 서버에 유저 상태 변경 요청 (JSON 형식)
     */
    public void setUserStatus(String userId, String status) {
        authServiceClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/oauth/status")
                        .queryParam("userInteractionState", status)
                        .build()
                )
                .header("X-User-ID", userId)
                .retrieve() // 2xx 응답이면 정상 처리, 4xx/5xx이면 예외 발생
                .bodyToMono(Void.class) // 응답 본문을 무시
                .block();
    }
}
