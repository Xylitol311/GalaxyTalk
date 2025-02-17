// apis.ts
import useFetcher from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';
import {
    AIQuestionsResponse,
    LetterFormValues,
    ParticipantsDataResponse,
    PreviousMessagesResponse,
    ReconnectDataResponse,
} from '../model/interfaces';

const { fetcher } = useFetcher();

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
export async function getPreviousMessages(chatRoomId: string) {
    const { data } = await fetcher.get<PreviousMessagesResponse>(
        PATH.API_PATH.CHAT.room(chatRoomId).GETMSG
    );
    return data;
}

// AI 질문 생성
export async function postAIQuestions(chatRoomId: string) {
    const { data } = await fetcher.post<AIQuestionsResponse>(
        PATH.API_PATH.CHAT.room(chatRoomId).AI
    );
    return data;
}

// 재연결
export async function postChatReconnect() {
    const { data } = await fetcher.post<ReconnectDataResponse>(
        PATH.API_PATH.CHAT.RECONNECT
    );
    return data;
}

// 참가자 정보 조회
export async function getChatParticipants(chatRoomId: string) {
    const { data } = await fetcher.get<ParticipantsDataResponse>(
        PATH.API_PATH.CHAT.room(chatRoomId).PARTICIPANTS
    );
    return data;
}

// 편지 보내기 함수
export async function postLetter(formData: LetterFormValues) {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.LETTER.CREATE,
        formData
    );
    return data;
}
