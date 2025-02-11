package com.example.chat.service;

import com.example.chat.dto.*;
import com.example.chat.entity.ChatMessage;
import com.example.chat.entity.Participant;
import com.example.chat.entity.ChatRoom;
import com.example.chat.repository.ChatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

    /**
     * 매칭된 두 유저 정보를 mongodb에 저장해 새로운 방을 생성합니다.
     * @param matchRequest
     * @return chatRoomId
     */
    @Transactional
    public String createChatRoom(MatchResultRequest matchRequest, String sessionId) throws JsonProcessingException {
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

        // 방을 생성하고 방의 Id를 가져옵니다.
        String chatRoomId = chatRepository.save(chatRoom).getId();

        // AI 질문을 방 생성 시점에 생성합니다.
        // createQuestions(chatRoomId, matchRequest.getConcern1(), matchRequest.getConcern2());

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
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + chatRoomId));
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
            int planetId = (Integer) externalApiService.getUserInfo(otherUserId).get("planetId");
            previousChatResponse.setParticipantPlanet(planetId);

            // TODO: Comment Server 구현 후 연동
            // previousChatResponse.setParticipantReview(externalApiService.getCommentByUserId(otherUserId));

            responseList.add(previousChatResponse);
        }

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
                .orElse(null);
    }

    /**
     * 방에 입력된 고민 두 개를 가지고 공통 질문 10개를 생성하고 mongodb에 저장합니다.
     * 이는 방 생성 시 호출됩니다.
     * @param concern1, concern2
     */
    public void createQuestions(String chatRoomId, String concern1, String concern2) throws JsonProcessingException {
        // 두 질문을 Prompt로 변환합니다.
        String prompt = createPromptwithTwoConcerns(concern1, concern2);

        // Prompt를 gpt api에 입력하고 질문 열 개를 Json 형태로 받아옵니다.
        String jsonString = externalApiService.createQuestions(prompt);

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
    }


    /**
     * 방 Id를 가지고 참여자들을 추출하고, 각 참여자들에 대한 정보를 Auth api에서 가져옵니다.
     * @param chatRoomId
     */
    public ParticipantsResponse getParticipantsInfo(String chatRoomId) {
        ParticipantsResponse participantsResponse = new ParticipantsResponse();

        // 방 정보에서 유사도 점수와 참가자 정보 추출
        ChatRoom chatRoom = chatRepository.findChatRoomById(chatRoomId);
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

        return participantsResponse;
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
