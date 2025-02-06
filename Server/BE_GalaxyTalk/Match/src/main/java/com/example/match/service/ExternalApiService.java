package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.SimilarityResponseDto;
import com.example.match.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalApiService {
    private final WebClient aiServiceClient;
    private final WebClient chatServiceClient;
    private final WebClient authServiceClient;

    public UserResponseDto getUserInfo(String userId) {
        return authServiceClient.get()
                .uri("/api/oauth?userId=" + userId)
                .retrieve()
                .bodyToMono(UserResponseDto.class)
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

        return aiServiceClient.post()
                .uri("/calculate-similarity")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SimilarityResponseDto.class)
                .map(SimilarityResponseDto::getSimilarityScore)
                .defaultIfEmpty(0.0)
                .block();
    }

    public void createChatRoom(List<String> userIds) {
        chatServiceClient.post()
                .uri("/room")
                .bodyValue(Map.of("userIds", userIds))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
