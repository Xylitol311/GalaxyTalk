package com.example.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class MatchResponseDto {
    private String type;
    private String message;
    private Map<String, Object> data;
}
