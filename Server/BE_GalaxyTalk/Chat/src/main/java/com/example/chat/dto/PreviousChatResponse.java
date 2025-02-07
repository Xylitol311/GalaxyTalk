package com.example.chat.dto;

import lombok.Data;

@Data
public class PreviousChatResponse {
    String chatRoomId;
    String myConcern;
    String participantConcern;
    String participantPlanet;
    String chatRoomCreatedAt;
    String participantReview;
}
