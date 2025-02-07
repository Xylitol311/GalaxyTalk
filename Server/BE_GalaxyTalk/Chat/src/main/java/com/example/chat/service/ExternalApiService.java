package com.example.chat.service;

import com.example.chat.dto.UserStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
        String requestBody = """
        {
            "model": "gpt-4o",
            "messages": [
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "%s"}
            ]
        }
        """.formatted(prompt);

        return gptClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)  // OpenAI 응답을 String으로 받음
                .block();
    }
}