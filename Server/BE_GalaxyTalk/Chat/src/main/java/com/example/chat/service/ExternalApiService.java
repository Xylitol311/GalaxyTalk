package com.example.chat.service;

import com.example.chat.dto.UserStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final WebClient authServiceClient;

    public void updateUserStatus(UserStatusRequest userStatusRequest) {
        authServiceClient.post()
            .uri("/status")
            .bodyValue(userStatusRequest)
            .retrieve()
            .bodyToMono(UserStatusRequest.class)
            .block();
    }
}