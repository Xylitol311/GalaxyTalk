package com.example.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "chat_rooms")
public class ChatRoom {
    @Id
    private String id;
    private String sessionId;
    private List<Participant> participants;
    private List<ChatMessage> messages;
    private Double similarityScore;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}