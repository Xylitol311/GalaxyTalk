package com.example.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SimilarityResponse {
    @JsonProperty("similarity_score")
    private double similarityScore;
}