package com.example.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class MatchResponseDto {
    private String type;
    private String message;
    private Map<String, Object> data;
}
