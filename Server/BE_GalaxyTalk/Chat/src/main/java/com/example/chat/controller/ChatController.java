package com.example.chat.controller;

import com.example.chat.dto.*;
import com.example.chat.entity.ChatMessage;
import com.example.chat.entity.ChatRoom;
import com.example.chat.entity.Participant;
import com.example.chat.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import jakarta.annotation.PostConstruct;
import livekit.LivekitModels.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    @Value("${livekit.url}")
    private String livekitUrl;

    private RoomServiceClient roomService;
    private final ChatService chatService;

    @PostConstruct
    public void init() {
        this.roomService = RoomServiceClient.create(livekitUrl, livekitApiKey, livekitApiSecret);
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponseDto> createSessionWithTwoTokens(@RequestBody MatchResultRequest matchResultRequest) throws IOException {
        // LiveKit room 생성
        String roomName = UUID.randomUUID().toString();

        // Room 생성 및 응답 처리
        Call<Room> roomCall = roomService.createRoom(roomName);
        Response<Room> response = roomCall.execute();
        Room room = response.body();

        if (room == null) {
            throw new RuntimeException("Failed to create LiveKit room");
        }

        // 채팅방 생성
        String chatRoomId = chatService.createChatRoom(matchResultRequest, roomName);

        // 첫 번째 사용자 토큰 생성
        String tokenA = generateToken(roomName, matchResultRequest.getUserId1());

        // 두 번째 사용자 토큰 생성
        String tokenB = generateToken(roomName, matchResultRequest.getUserId2());

        ChatResponse chatResponse = new ChatResponse(
                roomName,
                tokenA,
                tokenB,
                chatRoomId
        );

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "연결 성공",
                chatResponse
        ));
    }

    /**
     * 채팅 메시지 저장 API
     * @param chatRequest 저장할 메시지 정보
     * @return 저장된 메시지 객체
     */
    @PostMapping("/{chatRoomId}/message")
    public ResponseEntity<ApiResponseDto> saveMessage(@RequestBody ChatRequest chatRequest, @PathVariable String chatRoomId, @RequestHeader("X-User-Id") String userId) {
        chatRequest.setChatRoomId(chatRoomId);
        chatRequest.setSenderId(userId);

        ChatMessage savedMessage = chatService.saveMessage(chatRequest);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "메세지 전송 성공",
                null
        ));
    }

    /**
     * 이전 채팅 내용 조회 API
     * @param chatRoomId 채팅방 ID
     * @return 해당 채팅방의 모든 메시지 리스트
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponseDto> getPreviousMessages(@PathVariable String chatRoomId) {
        List<ChatMessage> messages = chatService.getPreviousMessages(chatRoomId);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "이전 대화 조회 성공",
                messages
        ));
    }

    /**
     * 채팅방 나가기 API
     * @param chatRoomId 나갈 채팅방 ID
     * @return 채팅방 나가기 처리 결과
     */
    @DeleteMapping("/{chatRoomId}/leave")
    public ResponseEntity<ApiResponseDto> leaveChat(@PathVariable String chatRoomId) {
        // 1. 채팅방의 세션 ID와 참여자 목록 조회
        ChatRoom chatRoom = chatService.getChatRoomWithParticipants(chatRoomId);

        // 2. 먼저 모든 참가자 강제 퇴장
        for (Participant participant : chatRoom.getParticipants()) {
            roomService.removeParticipant(chatRoom.getSessionId(), participant.getUserId());
        }

        // 3. LiveKit room 삭제
        roomService.deleteRoom(chatRoom.getSessionId());

        // 4. 채팅방 종료 처리
        chatService.endChatRoom(chatRoom);

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "채팅방 나가기 성공",
                null
        ));
    }


    /**
     * 채팅방 재연결
     * @param userId
     * @return sessionId, token
     */
    @PostMapping("/reconnect")
    public ResponseEntity<ApiResponseDto> reconnect(@RequestHeader("X-User-Id") String userId) {
        // 1. user가 속한 active room 찾기
        String roomId = chatService.getSessionId(userId);

        // 2. 새로운 토큰 생성
        String token = generateToken(roomId, userId);

        ReconnectResponse reconnectResponse = new ReconnectResponse(
                roomId,
                token
        );

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "재연결 성공",
                reconnectResponse
        ));
    }

    /**
     * 사용자의 이전 대화 내용에 대한 정보를 가져옵니다.
     * @param userId
     * @return chatRoomId, myConcern, participantConcern, participantPlanet, roomCreatedAt, participantReview
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponseDto> getPreviousChats(@RequestHeader("X-User-Id") String userId,
                                                           @RequestParam(value = "cursor", required = false) String cursor,
                                                           @RequestParam(value = "limit", defaultValue = "10") int limit) {

        Slice<PreviousChatResponse> sliceResult = chatService.getPreviousChat(userId, cursor, limit);

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "이전 대화 정보 요청 성공",
                CursorResponse.from(sliceResult, PreviousChatResponse::getChatRoomId)
        ));
    }

    /**
     * 요청 시 해당 방에 저장된 질문 10개를 전달합니다.
     * @param chatRoomId
     * @return questions
     */
    @GetMapping("/{chatRoomId}/ai")
    public ResponseEntity<ApiResponseDto> getQuestions(@PathVariable("chatRoomId") String chatRoomId) throws JsonProcessingException {
        List<Question> questions = chatService.getQuestions(chatRoomId);

        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "AI 질문 생성 성공",
                questions
        ));
    }

    /**
     * 세션과 property 정보를 받아 token을 생성합니다.
     */
    private String generateToken(String roomName, String participantId) {
        AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);
        token.setName(participantId);
        token.setIdentity(participantId);
        token.addGrants(new RoomJoin(true), new RoomName(roomName));

        return token.toJwt();
    }
}
