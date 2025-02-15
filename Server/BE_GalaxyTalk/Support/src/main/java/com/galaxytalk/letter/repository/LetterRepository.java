package com.galaxytalk.letter.repository;

import com.galaxytalk.letter.dto.Letter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LetterRepository extends JpaRepository<Letter, Long> {


    // 특정 chatRoomId 내에서 특정 senderId의 Letter 검색 ( 내가 보낸 건 채팅룸에서 확인)
    Letter findByChatRoomIdAndSenderId(String chatRoomId, String senderId);

    // receiverId를 기준으로 (내가 받은 편지들)
    List<Letter> findByReceiverId(String receiverId);

}
