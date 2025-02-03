package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
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
                "user1", user1,
                "user2", user2
        );

        return aiServiceClient.post()
                .uri("/similarity")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Double.class)
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
