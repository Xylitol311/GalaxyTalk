package com.example.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class MessageResponseDto {
    private String type;
    private String message;
    private Map<String, Object> data;
}
