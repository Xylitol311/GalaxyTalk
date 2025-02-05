package com.example.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MatchRequestDto {
    @NotBlank(message = "고민 내용은 필수입니다.")
    @Size(min = 10, max = 100, message = "고민 내용은 10자 이상 100자 이하로 작성해주세요.")
    private String concern;
    private String preferredMbti;
}
