package com.example.match.service;

import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MatchServiceIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private MatchService matchService;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private BlockingQueue<MatchResponseDto> blockingQueue;

    @BeforeEach
    void setup() {
        System.out.println("테스트 서버 포트: " + port);
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        this.stompClient = new WebSocketStompClient(new SockJsClient(transports));
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        this.blockingQueue = new LinkedBlockingQueue<>();
    }

    @Test
    void testMatchNotification() throws ExecutionException, InterruptedException, TimeoutException {
        // WebSocket 연결 URL 설정
        String wsUrl = String.format("http://localhost:%d/ws", port);

        // CompletableFuture를 사용한 비동기 연결
        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(wsUrl, new CustomStompSessionHandler());

        // 연결 완료 대기 (최대 5초)
        this.stompSession = sessionFuture.get(5, TimeUnit.SECONDS);

        // 테스트용 구독 설정
        String userId = "testUser";
        stompSession.subscribe("/topic/matching/" + userId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchResponseDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.add((MatchResponseDto) payload);
            }
        });

        // 매칭 시작 요청
        UserMatchStatus testUser = new UserMatchStatus();
        testUser.setUserId(userId);
        matchService.startMatching(testUser);

        // Awaitility를 사용한 비동기 응답 대기 및 검증
        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MatchResponseDto response = blockingQueue.poll();
                    assertNotNull(response, "매칭 응답을 받지 못했습니다");
                    assertEquals("WAITING", response.getType(), "잘못된 응답 타입입니다");
                    assertEquals("매칭 대기 시작", response.getMessage(), "잘못된 메시지입니다");
                });
    }
}

// 커스텀 StompSessionHandler
class CustomStompSessionHandler implements StompSessionHandler {
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("WebSocket 연결 성공!");
    }

    @Override
    public void handleException(StompSession session, StompCommand command,
                                StompHeaders headers, byte[] payload, Throwable exception) {
        System.err.println("WebSocket 에러 발생: " + exception.getMessage());
        exception.printStackTrace();
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        System.err.println("WebSocket 전송 에러: " + exception.getMessage());
        exception.printStackTrace();
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        // 일반적인 프레임 처리
    }
}