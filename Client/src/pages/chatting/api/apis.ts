// apis.ts
import { fetcher } from '@/app/api/axios';

// 메시지 전송
export async function postChatMessage(chatRoomId: string, content: string) {
    const { data } = await fetcher.post(`/api/chat/${chatRoomId}/message`, {
        content,
    });
    return data;
}

// 채팅방 나가기
export async function deleteChatRoom(chatRoomId: string) {
    const { data } = await fetcher.delete(`/api/chat/${chatRoomId}/leave`);
    return data;
}

// 메시지 목록 조회
export async function getChatMessages() {
    const { data } = await fetcher.get('/api/chat/messages');
    return data;
}

// AI 질문 생성
export async function postAIQuestions(chatRoomId: string) {
    const { data } = await fetcher.post(`/api/chat/${chatRoomId}/ai`);
    return data;
}

// 재연결
export async function postChatReconnect() {
    const { data } = await fetcher.post('/api/chat/reconnect');
    return data;
}

// 참가자 정보 조회
export async function getChatParticipants(chatRoomId: string) {
    const { data } = await fetcher.get(`/api/chat/${chatRoomId}/participants`);
    return data;
}
