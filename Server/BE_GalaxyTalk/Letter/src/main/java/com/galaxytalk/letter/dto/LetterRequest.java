package com.galaxytalk.letter.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LetterRequest {

    private String senderId;

    private String receiverId;

    private String content;

    private String chatRoomId;

}
