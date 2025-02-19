package com.example.chat.repository;

import com.example.chat.dto.Question;
import com.example.chat.entity.ChatRoom;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<ChatRoom, String> {
    @Query(value = "{ '_id' : ?0 }")
    ChatRoom findChatRoomById(String chatRoomId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'endedAt': ?1 }}")
    void updateEndedAt(String id, LocalDateTime now);

    @Query(value = "{ 'participants.userId': ?0, 'endedAt': null }", fields = "{ 'sessionId': 1}")
    Optional<ChatRoom> findActiveSessionIdByUserId(String userId);

    @Query(value = "{ 'participants.userId': ?0, 'endedAt': { $ne: null }, 'isCancelled': null, '_id': { $gt: ?1 } }",
            fields = "{ '_id': 1, 'createdAt': 1, 'participants': 1 }")
    Slice<ChatRoom> findAllChatDetailsByUserId(
            String userId,
            ObjectId cursor,
            Pageable pageable
    );

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'questions': ?1 }}")
    void updateQuestions(String roomId, List<Question> questions);

    @Query(value = "{ '_id': ?0 }", fields = "{ 'questions': 1 }")
    Optional<ChatRoom> findQuestionsByRoomId(String roomId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'isCancelled': true } }")
    void updateIsCancelled(String chatRoomId);

    // participants.userId가 ?1와 일치하고, 
    // _id가 ?0와 같지 않고 endedAt이 null인 조건을 만족하는 문서의 
    // endedAt을 현재 시각으로, isCancelled를 true로 업데이트
    @Query(value = "{ 'participants.userId': ?1, '_id': { $ne: ?0 }, 'endedAt': null }")
    @Update("{ '$currentDate': { 'endedAt': true }, '$set': { 'isCancelled': true } }")
    void updateAbnormalChatrooms(String chatRoomId, String userId);
}
