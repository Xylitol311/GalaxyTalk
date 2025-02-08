package com.example.chat.service;

import com.example.chat.dto.UserStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final WebClient authServiceClient;

    private final WebClient commentServiceClient;

    private final WebClient gptClient;

    // Auth server에 유저 정보가 idle 또는 chatting으로 변경됨을 알림
    public void updateUserStatus(UserStatusRequest userStatusRequest) {
        authServiceClient.post()
            .uri("/status")
            .bodyValue(userStatusRequest)
            .retrieve()
            .bodyToMono(UserStatusRequest.class)
            .block();
    }

    // Auth server에서 상대방 id를 가지고 상대방 행성 이름 가져오기
    public String getUserPlanet(String userId) {
        return authServiceClient.get()
                .uri("/planets/" + userId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // Comment server에서 내가 남긴 상대에 대한 리뷰 받아오기
    public String getCommentByUserId(String userId) {
        return commentServiceClient.get()
                .uri("/review/" + userId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // GPT 통해 10개의 질문 생성
    public String createQuestions(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        return gptClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 최대 3번 재시도, 1초 간격
                        .filter(throwable -> throwable instanceof WebClientResponseException) // WebClient 예외만 재시도
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new RuntimeException("GPT API 호출 실패: 최대 재시도 횟수 초과", retrySignal.failure());
                        }))
                .block();
    }
}