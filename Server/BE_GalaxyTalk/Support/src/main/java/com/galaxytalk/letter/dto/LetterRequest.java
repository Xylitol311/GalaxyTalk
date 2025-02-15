package com.galaxytalk.letter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LetterRequest {

    private String senderId;

    private String receiverId;

    private String content;

    private String chatRoomId;

}
