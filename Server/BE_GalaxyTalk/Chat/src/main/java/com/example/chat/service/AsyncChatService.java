package com.example.chat.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.chat.dto.Question;
import com.example.chat.repository.ChatRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class AsyncChatService {

    @Value("${prompt}")
    private String questionsPrompt;
    private final ExternalApiService externalApiService;
    private final ChatRepository chatRepository;

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

            log.info("chatgpt 생성 질문: {}", content);
            List<Question> questions = new ArrayList<>();

            try {
                questions = objectMapper.readValue(content, new TypeReference<List<Question>>() {});
            } catch (JsonParseException e) {
                log.info("질문이 제대로 생성되지 않음.");
                Question question = new Question();
                question.setQuestionId("1");
                question.setContent("유사도가 낮아 질문을 생성할 수 없습니다.");
                questions.add(question);
            }

            // 질문을 mongodb에 저장합니다.
            chatRepository.updateQuestions(chatRoomId, questions);

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private String createPromptwithTwoConcerns(String concern1, String concern2) {
        return String.format(questionsPrompt, concern1, concern2);
    }
}
