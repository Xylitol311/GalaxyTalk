package com.galaxytalk.feedback.service;

import com.galaxytalk.feedback.dto.FeedbackRequestDto;
import com.galaxytalk.feedback.entity.Feedback;
import com.galaxytalk.feedback.repository.FeedbackRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    @Transactional
    public void saveFeedback(String serialNumber, FeedbackRequestDto requestDto) {
        Feedback feedback = Feedback.builder()
                .writerId(serialNumber)
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .build();

        feedbackRepository.save(feedback);
    }
}
