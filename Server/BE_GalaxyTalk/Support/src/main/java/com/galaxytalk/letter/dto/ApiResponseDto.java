package com.galaxytalk.letter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// 프론트 응답용 DTO
@Data
@AllArgsConstructor
public class ApiResponseDto {

    private boolean success;
    private String message;
    private Object data;


}

