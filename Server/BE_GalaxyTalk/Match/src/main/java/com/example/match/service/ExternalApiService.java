package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResultRequestDto;
import com.example.match.dto.SimilarityResponseDto;
import com.example.match.dto.UserStatusRequestDto;
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
    private final WebClient authServiceClient;
    private final WebClient chatServiceClient;

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
     * - Match 서버에서 유저 정보, 고민 내용, 유사도 점수를 Chat 서버로 전달하여
     * 채팅방 생성 후 sessionId, token, chatRoomId 반환
     */
    public Map<String, Object> createChatRoom(UserMatchStatus user1, UserMatchStatus user2, double similarityScore) {
        log.info("채팅방 생성 요청");
        MatchResultRequestDto matchResultRequestDto = new MatchResultRequestDto();
        matchResultRequestDto.setConcern1(user1.getConcern());
        matchResultRequestDto.setConcern2(user2.getConcern());
        matchResultRequestDto.setUserId1(user1.getUserId());
        matchResultRequestDto.setUserId2(user2.getUserId());
        matchResultRequestDto.setSimilarityScore(similarityScore);
        Map<String, Object> response = chatServiceClient.post()
                .uri("/api/chat/match")
                .bodyValue(matchResultRequestDto)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return (Map<String, Object>) response.get("data");
    }

    /**
     * Auth 서버에 유저 상태 변경 요청 (idle 또는 chatting)
     */
    public void setUserStatus(String userId, String status) {
        log.info("유저 상태 변경 요청: {} -> {}", userId, status);
        UserStatusRequestDto request = new UserStatusRequestDto();
        request.setUserInteractionState(status);
        authServiceClient.post()
                .uri("/api/oauth/status")
                .header("X-User-ID", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    /**
     * Auth 서비스에서 유저 정보 조회
     */
    public Map<String, Object> getUserInfo(String userId) {
        log.info("유저 정보 조회 요청: {}", userId);
        Map<String, Object> response = authServiceClient.get()
                .uri("/api/oauth")
                .header("X-User-ID", userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return (Map<String, Object>) response.get("data");
    }
}
