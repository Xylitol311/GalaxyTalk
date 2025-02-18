package com.galaxytalk.letter.service;

import com.galaxytalk.letter.dto.Letter;
import com.galaxytalk.letter.dto.LetterRequest;
import com.galaxytalk.letter.repository.LetterRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LetterService {

    private final LetterRepository letterRepository;

    public LetterService(LetterRepository letterRepository) {
        this.letterRepository = letterRepository;
    }

    @Transactional
    public void saveLetter(String serialNumber,LetterRequest letter) {
        letterRepository.save(new Letter(serialNumber, letter.getReceiverId(), letter.getContent(), letter.getChatRoomId()));
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
    @Transactional
    public boolean hideLetter(long letterId) {
        return letterRepository.findById(letterId)
                .map(letter -> {
                    letter.setIsHide(1);
                    letterRepository.save(letter);  // 변경 사항 저장
                    return true;
                })
                .orElse(false);
    }




}
