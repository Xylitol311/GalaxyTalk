package com.example.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ParticipantsResponse {
    List<ParticipantInfo> participants;
    Double similarity;
}
