package com.example.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
}