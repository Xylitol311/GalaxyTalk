// apis.ts
import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';

// 메시지 전송
export async function postChatMessage(chatRoomId: string, content: string) {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.CHAT.room(chatRoomId).SENDMSG,
        { content }
    );
    return data;
}

// 채팅방 나가기
export async function deleteChatRoom(chatRoomId: string) {
    const { data } = await fetcher.delete<BaseResponseType>(
        PATH.API_PATH.CHAT.room(chatRoomId).LEAVE
    );

    return data;
}

// 메시지 목록 조회
export async function getChatMessages() {
    const { data } = await fetcher.get<BaseResponseType>(
        PATH.API_PATH.CHAT.GETMSG
    );
    return data;
}

// AI 질문 생성
export async function postAIQuestions(chatRoomId: string) {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.CHAT.room(chatRoomId).AI
    );
    return data;
}

// 재연결
export async function postChatReconnect() {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.CHAT.RECONNECT
    );
    return data;
}

// 참가자 정보 조회
export async function getChatParticipants(chatRoomId: string) {
    const { data } = await fetcher.get<BaseResponseType>(
        PATH.API_PATH.CHAT.room(chatRoomId).PARTICIPANTS
    );
    return data;
}
