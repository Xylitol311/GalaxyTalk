package com.example.match.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor  // Jackson이 역직렬화할 때 사용
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON에 포함하지 않음
public class MessageResponseDto {
    private String type;
    private String message;
    private Map<String, Object> data;
}
