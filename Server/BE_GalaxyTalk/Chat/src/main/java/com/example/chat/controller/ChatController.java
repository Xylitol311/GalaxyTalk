package com.example.chat.controller;

import com.example.chat.dto.*;
import com.example.chat.entity.ChatMessage;
import com.example.chat.entity.ChatRoom;
import com.example.chat.entity.Participant;
import com.example.chat.exception.BusinessException;
import com.example.chat.service.ChatService;
import com.example.chat.exception.ErrorCode;
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
        log.info("LiveKit room 생성 요청: roomName={}", roomName);

        // Room 생성 및 응답 처리
        Call<Room> roomCall = roomService.createRoom(roomName);
        Response<Room> response = roomCall.execute();
        Room room = response.body();

        // 에러 처리: LiveKit 방 생성 실패 시
        if (room == null) {
            log.error("LiveKit 방 생성 실패: room is null, roomName={}", roomName);
            throw new BusinessException(ErrorCode.LIVEKIT_ROOM_CREATION_FAILED);
        }

        // 채팅방 생성
        String chatRoomId = chatService.createChatRoom(matchResultRequest, roomName);
        log.info("채팅방 생성 성공: chatRoomId={}, sessionId={}", chatRoomId, roomName);

        // 첫 번째 사용자 토큰 생성
        String tokenA = generateToken(roomName, matchResultRequest.getUserId1());

        // 두 번째 사용자 토큰 생성
        String tokenB = generateToken(roomName, matchResultRequest.getUserId2());

        log.info("토큰 생성 완료: user1, user2 토큰 생성");

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
        log.info("메시지 저장 성공: chatRoomId={}, senderId={}", chatRoomId, userId);

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
        log.info("이전 메시지 조회 성공: chatRoomId={}", chatRoomId);
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

        if (chatRoom == null) {
            log.error("채팅방 나가기 실패: 채팅방 {} 존재하지 않음", chatRoomId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        // 2. 먼저 모든 참가자 강제 퇴장
        for (Participant participant : chatRoom.getParticipants()) {
            log.info("참가자 {} 강제 퇴장 시도: sessionId={}", participant.getUserId(), chatRoom.getSessionId());
            roomService.removeParticipant(chatRoom.getSessionId(), participant.getUserId());
        }

        // 3. LiveKit room 삭제
        log.info("LiveKit room 삭제 시도: sessionId={}", chatRoom.getSessionId());
        roomService.deleteRoom(chatRoom.getSessionId());

        // 4. 채팅방 종료 처리
        chatService.endChatRoom(chatRoom);
        log.info("채팅방 종료 처리 완료: chatRoomId={}", chatRoomId);

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
        ChatRoom chatRoom = chatService.getSessionId(userId);

        if (chatRoom == null) {
            log.error("재연결 실패: 활성 채팅방 없음 for userId={}", userId);
            throw new BusinessException(ErrorCode.ACTIVE_CHAT_ROOM_NOT_FOUND);
        }

        String chatRoomId = chatRoom.getId();
        String sessionId = chatRoom.getSessionId();

        // 2. 새로운 토큰 생성
        String token = generateToken(sessionId, userId);
        log.info("재연결 토큰 생성 성공: userId={}, sessionId={}", userId, sessionId);

        ReconnectResponse reconnectResponse = new ReconnectResponse(
                chatRoomId,
                sessionId,
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
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponseDto> getPreviousChats(@RequestHeader("X-User-Id") String userId,
                                                           @RequestParam(value = "cursor", required = false) String cursor,
                                                           @RequestParam(value = "limit", defaultValue = "10") int limit) {

        Slice<PreviousChatResponse> sliceResult = chatService.getPreviousChat(userId, cursor, limit);

        log.info("이전 채팅 목록 조회 성공: userId={}", userId);
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
    public ResponseEntity<ApiResponseDto> getQuestions(@PathVariable("chatRoomId") String chatRoomId) {
        List<Question> questions = chatService.getQuestions(chatRoomId);

        if (questions == null || questions.isEmpty()) {
            log.error("AI 질문 생성 실패: 채팅방 {}의 질문이 존재하지 않음", chatRoomId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_QUESTION_NOT_FOUND);
        }

        log.info("AI 질문 조회 성공: chatRoomId={}", chatRoomId);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "AI 질문 생성 성공",
                questions
        ));
    }

    /**
     * 참여자들의 정보(UserId & 고민 & MBTI & 매너온도 2개, 유사도) 반환
     * @param chatRoomId
     */
    @GetMapping("/{chatRoomId}/participants")
    public ResponseEntity<ApiResponseDto> getParticipants(@PathVariable("chatRoomId") String chatRoomId) {
        ParticipantsResponse participants = chatService.getParticipantsInfo(chatRoomId);

        if (participants == null) {
            log.error("참가자 정보 조회 실패: 채팅방 {}의 참가자 정보 없음", chatRoomId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        log.info("참가자 정보 조회 성공: chatRoomId={}", chatRoomId);
        return ResponseEntity.ok(new ApiResponseDto(
                true,
                "참가자 정보 조회 성공",
                participants
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
