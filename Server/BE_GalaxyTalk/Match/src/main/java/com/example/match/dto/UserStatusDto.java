package com.example.match.dto;

import com.example.match.domain.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusDto {
    private String userId;
    private String concern;
    private String prefferedMbti;
    private MatchStatus status;
    private long startTime;
}
