package com.example.chat.service;

import com.example.chat.dto.*;
import com.example.chat.entity.ChatMessage;
import com.example.chat.entity.Participant;
import com.example.chat.entity.ChatRoom;
import com.example.chat.exception.BusinessException;
import com.example.chat.exception.ErrorCode;
import com.example.chat.repository.ChatRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    @Value("${prompt}")
    private String questionsPrompt;

    private final MongoTemplate mongoTemplate;
    private final ChatRepository chatRepository;
    private final ExternalApiService externalApiService;
    private final AsyncChatService asyncChatService;

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

        // 채팅방 저장 후 채팅방 ID가 없으면 에러 처리
        ChatRoom savedRoom = chatRepository.save(chatRoom);
        if (savedRoom.getId() == null) {
            log.error("채팅방 저장 실패: sessionId={}", sessionId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_SAVE_FAILED);
        }
        String chatRoomId = savedRoom.getId();

        // AI 질문을 방 생성 시점에 생성합니다.
//        asyncChatService.createQuestions(chatRoomId, matchRequest.getConcern1(), matchRequest.getConcern2())
//                .exceptionally(throwable -> {
//                    log.error("질문 생성 중 에러 발생", throwable);
//                    return null;
//                });

        return chatRoomId;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    public ChatRoom getChatRoomWithParticipants(String chatRoomId) {
        return chatRepository.findChatRoomById(chatRoomId);
    }

    @Transactional
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
    public ChatRoom getSessionId(String userId) {
        return chatRepository.findActiveSessionIdByUserId(userId) // ChatRoom 객체에서 sessionId만 추출
            .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVE_CHAT_ROOM_NOT_FOUND));
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

        // 이전 대화가 하나도 없는 경우
        if(chatRooms == null) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        List<PreviousChatResponse> responseList = new ArrayList<>();

        for (ChatRoom chatRoom : chatRooms) {
            PreviousChatResponse previousChatResponse = new PreviousChatResponse();
            previousChatResponse.setChatRoomId(chatRoom.getId());
            previousChatResponse.setChatRoomCreatedAt(chatRoom.getCreatedAt().toString());

            List<Participant> participants = chatRoom.getParticipants();

            // 내 id가 채팅방에 없다면 예외 발생
            Participant me = participants.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_INFO_NOT_FOUND));

            // 상대방 id가 채팅방에 없다면 예외 발생
            Participant other = participants.stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_INFO_NOT_FOUND));

            String otherUserId = other.getUserId();

            // 상대방 ID set
            previousChatResponse.setParticipantId(otherUserId);

            // 내 고민 set
            previousChatResponse.setMyConcern(me.getConcern());

            // 상대방 고민 set
            previousChatResponse.setParticipantConcern(other.getConcern());

            // 상대방 행성 set
            int planetId = (Integer) externalApiService.getUserInfo(otherUserId).get("planetId");
            previousChatResponse.setParticipantPlanet(planetId);

            // support api에서 후기 가져와 상대방에 대한 내 후기 set
            Map<String, Object> response = externalApiService.getLetter(otherUserId, chatRoom.getId());

            // 후기를 조회할 수 없는 경우
            if(response == null) {
                throw new BusinessException(ErrorCode.LETTER_NOT_FOUND);
            }

            String letter = (String) response.get("content");

            previousChatResponse.setParticipantReview(letter);

            responseList.add(previousChatResponse);
        }

        log.info("이전 채팅 목록 조회 완료: userId={}", userId);
        return new SliceImpl<>(responseList, pageRequest, chatRooms.hasNext());
    }

    /**
     * createQuestions() 메서드를 통해 생성된 질문을 mongodb에서 가져와 반환합니다.
     * @param chatRoomId
     * @return questions
     */
    public List<Question> getQuestions(String chatRoomId) {
        return chatRepository.findQuestionsByRoomId(chatRoomId)
                .map(ChatRoom::getQuestions)  // ChatRoom에서 questions 리스트를 가져옴
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_QUESTION_NOT_FOUND));
    }

    /**
     * 방 Id를 가지고 참여자들을 추출하고, 각 참여자들에 대한 정보를 Auth api에서 가져옵니다.
     * @param chatRoomId
     */
    public ParticipantsResponse getParticipantsInfo(String chatRoomId) {
        ParticipantsResponse participantsResponse = new ParticipantsResponse();

        // 방 정보에서 유사도 점수와 참가자 정보 추출
        ChatRoom chatRoom = chatRepository.findChatRoomById(chatRoomId);

        // 일치하는 채팅방이 없는 경우
        if (chatRoom == null) {
            log.error("참가자 정보 조회 실패: 채팅방 {}이 없음", chatRoomId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        List<Participant> participants = chatRoom.getParticipants();

        // 반환 형식에 일치하는 DTO 리스트 형태
        List<ParticipantInfo> participantsList = new ArrayList<>();
        for(Participant participant : participants) {
            String userId = participant.getUserId();

            // auth 서버에서 값 추출
            int planetId = (Integer) externalApiService.getUserInfo(userId).get("planetId");
            String mbti = (String) externalApiService.getUserInfo(userId).get("mbti");
            int energy = (Integer) externalApiService.getUserInfo(userId).get("energy");

            // 고민 추출
            String concern = participant.getConcern();

            // DTO 생성 후 리스트 add
            participantsList.add(new ParticipantInfo(
                    userId,
                    mbti,
                    concern,
                    planetId,
                    energy
            ));
        }

        participantsResponse.setParticipants(participantsList);
        participantsResponse.setSimilarity(chatRoom.getSimilarityScore());

        log.info("참가자 정보 조회 성공: chatRoomId={}", chatRoomId);
        return participantsResponse;
    }

    /**
     * 방에 입력된 고민 두 개를 가지고 공통 질문 10개를 생성하고 mongodb에 저장합니다.
     * 이는 방 생성 시 호출됩니다.
     * @param concern1, concern2
     */
    @Async
    public CompletableFuture<Void> createQuestions(String chatRoomId, String concern1, String concern2) {
        try {
            // 두 질문을 Prompt로 변환합니다.
            String prompt = createPromptwithTwoConcerns(concern1, concern2);

            // Prompt를 gpt api에 입력하고 질문 열 개를 Json 형태로 받아옵니다.
            String jsonString = externalApiService.createQuestions(prompt);

            // jsonString이 비어있으면 에러 처리
            if (jsonString == null || jsonString.isEmpty()) {
                log.error("질문 생성 실패: GPT API 응답 없음, chatRoomId={}", chatRoomId);
                throw new BusinessException(ErrorCode.GPT_API_FAILED);
            }

            // 1. OpenAI API 응답 전체를 JSON 노드로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonString);

            // 2. "choices" 배열에서 첫 번째 요소의 "message.content" 추출
            String content = rootNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            List<Question> questions = objectMapper.readValue(content, new TypeReference<List<Question>>() {});

            // 질문을 mongodb에 저장합니다.
            chatRepository.updateQuestions(chatRoomId, questions);

            log.info("질문 생성 완료: chatRoomId={}", chatRoomId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String createPromptwithTwoConcerns(String concern1, String concern2) {
        return String.format(questionsPrompt, concern1, concern2);
    }

    private void updateUserStatus(String userId, String status) {
        externalApiService.updateUserStatus(new UserStatusRequest(
                userId,
                status
        ));
    }

}
