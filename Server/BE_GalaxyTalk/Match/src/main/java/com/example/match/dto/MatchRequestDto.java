package com.example.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MatchRequestDto {
    @NotBlank(message = "고민 내용은 필수입니다.")
    @Size(min = 10, max = 100, message = "고민 내용은 10자 이상 100자 이하로 작성해주세요.")
    private String concern;

    @NotBlank(message = "선호하는 MBTI는 필수입니다.")
    @Size(min = 4, max = 4, message = "MBTI는 4자리여야 합니다.")
    private String preferredMbti;
}
