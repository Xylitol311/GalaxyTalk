package com.example.chat.repository;

import com.example.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ChatRepository extends MongoRepository<ChatRoom, String> {
    @Query(value = "{ '_id' : ?0 }")
    ChatRoom findChatRoomById(String chatRoomId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'endedAt': ?1 }}")
    void updateEndedAt(String id, LocalDateTime now);
}