package com.example.chat.service;

import com.example.chat.dto.UserStatusRequest;
import com.example.chat.entity.ChatRoom;
import com.example.chat.repository.ChatRepository;
import com.example.chat.entity.Participant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ExternalApiService externalApiService;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("채팅방 종료 시 종료시간이 기록되고 유저 상태가 idle로 변경된다")
    void endChatRoom_ShouldUpdateEndTimeAndUserStatus() {
        // given
        ChatRoom chatRoom = createChatRoom();

        ArgumentCaptor<String> chatRoomIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> endedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<UserStatusRequest> userStatusCaptor = ArgumentCaptor.forClass(UserStatusRequest.class);

        // when
        chatService.endChatRoom(chatRoom);

        // then
        // 채팅방 종료 시간 업데이트 검증
        verify(chatRepository).updateEndedAt(
                chatRoomIdCaptor.capture(),
                endedAtCaptor.capture()
        );
        assertEquals(chatRoom.getId(), chatRoomIdCaptor.getValue());
        assertNotNull(endedAtCaptor.getValue());

        // 유저 상태 변경 API 호출 검증
        verify(externalApiService, times(2))
                .updateUserStatus(userStatusCaptor.capture());

        List<UserStatusRequest> capturedRequests = userStatusCaptor.getAllValues();
        assertEquals(2, capturedRequests.size());

        // 첫 번째 참가자 상태 변경 검증
        assertEquals("user1", capturedRequests.get(0).getUserId());
        assertEquals("idle", capturedRequests.get(0).getStatus());

        // 두 번째 참가자 상태 변경 검증
        assertEquals("user2", capturedRequests.get(1).getUserId());
        assertEquals("idle", capturedRequests.get(1).getStatus());
    }

    private ChatRoom createChatRoom() {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId("chatRoom1");

        Participant participant1 = new Participant();
        participant1.setUserId("user1");

        Participant participant2 = new Participant();
        participant2.setUserId("user2");

        chatRoom.setParticipants(Arrays.asList(participant1, participant2));

        return chatRoom;
    }
}