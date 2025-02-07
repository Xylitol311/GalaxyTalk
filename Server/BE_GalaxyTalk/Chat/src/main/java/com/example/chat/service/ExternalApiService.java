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
}