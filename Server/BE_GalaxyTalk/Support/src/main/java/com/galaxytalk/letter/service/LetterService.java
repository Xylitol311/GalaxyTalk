package com.galaxytalk.letter.service;

import com.galaxytalk.letter.dto.Letter;
import com.galaxytalk.letter.repository.LetterRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LetterService {

    private final LetterRepository letterRepository;

    public LetterService(LetterRepository letterRepository) {
        this.letterRepository = letterRepository;
    }

    @Transactional
    public void saveLetter(Letter letter) {
        letterRepository.save(new Letter(letter.getSenderId(), letter.getReceiverId(), letter.getContent(), letter.getChatRoomId()));
    }

    //내가 쓴 편지, 채팅룸 따라
    public Letter getChatletter(String chatRoomId, String senderId){
       return letterRepository.findByChatRoomIdAndSenderId(chatRoomId, senderId);
    }

    //내가 받은 편지 목록들
    public List<Letter> getLetters(String receiverId){
        return letterRepository.findByReceiverIdAndIsHide(receiverId,0);
    }

    //편지 하나씩 열람
    public Letter getAletter(Long letterId){
        return letterRepository.findById(letterId).orElse(null);
    }


}
