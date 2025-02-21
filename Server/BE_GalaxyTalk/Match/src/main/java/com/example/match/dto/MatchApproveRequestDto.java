package com.example.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchApproveRequestDto {
    @NotBlank(message = "matchId는 필수입니다.")
    private String matchId;
    @NotNull(message = "수락 여부는 필수입니다.")
    private boolean accepted;
}
