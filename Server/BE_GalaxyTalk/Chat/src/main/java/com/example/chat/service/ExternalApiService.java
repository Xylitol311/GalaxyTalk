package com.example.chat.service;

import com.example.chat.dto.UserStatusRequest;
import com.example.chat.exception.BusinessException;
import com.example.chat.exception.ErrorCode;
import com.example.chat.feign.AuthClient;
import com.example.chat.feign.LetterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
@Slf4j
public class ExternalApiService {

    private final AuthClient authServiceClient;

    private final LetterClient supportServiceClient;

    private final WebClient gptClient;

    // Auth server에 유저 정보가 idle 또는 chatting으로 변경됨을 알림
    public void updateUserStatus(UserStatusRequest userStatusRequest) {
        ResponseEntity<?> response = authServiceClient.changeUserStatus(userStatusRequest.getUserId(), userStatusRequest.getStatus());
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            log.error("사용자 상태 업데이트 실패: userId={}, status={}", userStatusRequest.getUserId(), userStatusRequest.getStatus());
            throw new BusinessException(ErrorCode.USER_STATUS_UPDATE_FAILED);
        }
        log.info("사용자 상태 업데이트 성공: userId={}, status={}", userStatusRequest.getUserId(), userStatusRequest.getStatus());
    }

    // Auth server에서 상대방 id를 가지고 상대방 정보를 가져옴
    // 행성 ID, MBTI, 매너온도 데이터 추출
    public Map<String, Object> getUserInfo(String userId) {
        ResponseEntity<?> responseEntity = authServiceClient.getUser(userId);

        if (responseEntity == null || responseEntity.getBody() == null) {
            log.error("유저 정보 조회 실패: userId={} 응답 없음", userId);
            throw new BusinessException(ErrorCode.USER_INFO_NOT_FOUND);
        }

        Map<String, Object> response = (Map<String, Object>) responseEntity.getBody();
        return (Map<String, Object>) response.get("data");
    }

    // support server에서 내가 남긴 상대에 대한 리뷰 받아오기
    public Map<String, Object> getLetter(String userId, String chatRoomId) {
        ResponseEntity<?> responseEntity = supportServiceClient.getLetter(userId, chatRoomId);
        if (responseEntity == null || responseEntity.getBody() == null) {
            log.error("리뷰 정보 조회 실패: userId={}, chatRoomId={}", userId, chatRoomId);
            throw new BusinessException(ErrorCode.LETTER_NOT_FOUND);
        }
        
        log.info("리뷰 정보 조회 요청: userId={}, chatRoomId={}", userId, chatRoomId);
        Map<String, Object> response = (Map<String, Object>) responseEntity.getBody();
        return (Map<String, Object>) response.get("data");
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
                            throw new BusinessException(ErrorCode.GPT_API_FAILED);
                        }))
                .block();
    }
}