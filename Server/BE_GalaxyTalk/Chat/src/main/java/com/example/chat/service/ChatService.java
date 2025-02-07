package com.example.chat.service;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.PreviousChatResponse;
import com.example.chat.dto.UserStatusRequest;
import com.example.chat.entity.ChatMessage;
import com.example.chat.dto.MatchResultRequest;
import com.example.chat.entity.Participant;
import com.example.chat.entity.ChatRoom;
import com.example.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final MongoTemplate mongoTemplate;
    private final ChatRepository chatRepository;
    private final ExternalApiService externalApiService;

    /**
     * 매칭된 두 유저 정보를 mongodb에 저장해 새로운 방을 생성합니다.
     * @param matchRequest
     * @return chatRoomId
     */
    public String createChatRoom(MatchResultRequest matchRequest, String sessionId) {
        ChatRoom chatRoom = new ChatRoom();

        // participants 설정
        List<Participant> participants = new ArrayList<>();

        Participant participant1 = new Participant();
        participant1.setUserId(matchRequest.getUserId1());
        participant1.setConcern(matchRequest.getConcern1());

        Participant participant2 = new Participant();
        participant2.setUserId(matchRequest.getUserId2());
        participant2.setConcern(matchRequest.getConcern2());

        participants.add(participant1);
        participants.add(participant2);

        // 빈 메시지 리스트 초기화
        List<ChatMessage> messages = new ArrayList<>();

        // ChatRoom 설정
        chatRoom.setSessionId(sessionId);
        chatRoom.setParticipants(participants);
        chatRoom.setMessages(messages);
        chatRoom.setSimilarityScore(matchRequest.getSimilarityScore());
        chatRoom.setCreatedAt(LocalDateTime.now());
        
        // 두 사용자 auth api에 채팅 상태로 변경 요청
        updateUserStatus(participant1.getUserId(), "chatting");
        updateUserStatus(participant2.getUserId(), "chatting");

        return chatRepository.save(chatRoom).getId();
    }

    /**
     * 새로운 채팅 메시지를 저장
     * @param chatRequest 저장할 메시지 정보를 담은 DTO
     * @return 저장된 메시지 객체
     */
    public ChatMessage saveMessage(ChatRequest chatRequest) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(chatRequest.getSenderId());
        message.setContent(chatRequest.getContent());
        message.setCreatedAt(LocalDateTime.now());

        // chat_rooms 컬렉션에서 특정 document(messages)에 새 메시지 추가
        Update update = new Update().push("messages", message);

        // ObjectId(ChatRoomId)를 이용해 채팅방을 찾아 메시지 추가
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(new ObjectId(chatRequest.getChatRoomId()))),
                update,
                "chat_rooms"
        );

        log.info("Saving chat message for chatRoom {}: {}", chatRequest.getChatRoomId(), chatRequest.getContent());
        return message;
    }

    /**
     * 특정 세션의 이전 채팅 내용을 조회
     * @param chatRoomId 채팅방 ID
     * @return 해당 세션의 모든 메시지 리스트
     */
    public List<ChatMessage> getPreviousMessages(String chatRoomId) {
        log.info("Retrieving previous messages for chatRoom: {}", chatRoomId);
        return chatRepository.findById(chatRoomId)
                .map(chatRoom -> chatRoom.getMessages().stream()
                        .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + chatRoomId));
    }

    public ChatRoom getChatRoomWithParticipants(String chatRoomId) {
        return chatRepository.findChatRoomById(chatRoomId);
    }

    public void endChatRoom(ChatRoom chatRoom) {
        // 종료 시간 기록
        chatRepository.updateEndedAt(chatRoom.getId(), LocalDateTime.now());

        String participant1 = chatRoom.getParticipants().get(0).getUserId();
        String participant2 = chatRoom.getParticipants().get(1).getUserId();

        // 두 유저 상태 변경 auth api에 idle로 변경 요청
        updateUserStatus(participant1, "idle");
        updateUserStatus(participant2, "idle");
    }

    /**
     * user가 속한 active sessionId를 가져옵니다.
     * @param userId
     * @return sessionId
     */
    public String getSessionId(String userId) {
        return chatRepository.findActiveSessionIdByUserId(userId)
            .map(ChatRoom::getSessionId)  // ChatRoom 객체에서 sessionId만 추출
            .orElse(null);
    }

    /**
     * 내가 했던 채팅방 정보를 가져옵니다.
     * 이 때 endedAt이 Null이 아닌, 종료된 대화의 채팅만 가져옵니다.
     * pagination 객체 형태로 전달합니다.
     * @param userId, page, size
     * @return Page<PreviousChatResponse>
     */
    public Slice<PreviousChatResponse> getPreviousChat(String userId, String cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1, Sort.by("_id").ascending());

        // cursor가 null이거나 빈 문자열인 경우 처리
        ObjectId objectIdCursor = (cursor != null && !cursor.trim().isEmpty())
                ? new ObjectId(cursor) : new ObjectId("000000000000000000000000");

        Slice<ChatRoom> chatRooms = chatRepository.findAllChatDetailsByUserId(
                userId,
                objectIdCursor,
                pageRequest
        );

        List<PreviousChatResponse> responseList = new ArrayList<>();

        for (ChatRoom chatRoom : chatRooms) {
            PreviousChatResponse previousChatResponse = new PreviousChatResponse();
            previousChatResponse.setChatRoomId(chatRoom.getId());
            previousChatResponse.setChatRoomCreatedAt(chatRoom.getCreatedAt().toString());

            List<Participant> participants = chatRoom.getParticipants();

            Participant me = participants.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow();

            Participant other = participants.stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow();

            String otherUserId = other.getUserId();
            previousChatResponse.setMyConcern(me.getConcern());
            previousChatResponse.setParticipantConcern(other.getConcern());

            previousChatResponse.setParticipantPlanet(externalApiService.getUserPlanet(otherUserId));
            previousChatResponse.setParticipantReview(externalApiService.getCommentByUserId(otherUserId));

            responseList.add(previousChatResponse);
        }

        return new SliceImpl<>(responseList, pageRequest, chatRooms.hasNext());
    }

    private void updateUserStatus(String userId, String status) {
        externalApiService.updateUserStatus(new UserStatusRequest(
                userId,
                status
        ));
    }
}
