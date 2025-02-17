import { BaseResponseType } from '@/app/model/types/api';

// 기본이 되는 메시지 내용 인터페이스
export interface BaseMessage {
    content: string;
}
// 수신 및 조회용 메시지 인터페이스
export interface Message extends BaseMessage {
    senderId: string;
    createdAt: string;
}

export interface PreviousMessagesResponse extends BaseResponseType {
    data: Message[];
}

export interface AIQuestion {
    questionId: string;
    content: string;
}

// AI 질문 객체 타입 정의
export interface AIQuestionsResponse extends BaseResponseType {
    data: AIQuestion[];
}

export interface ChatData {
    sessionId: string; // 생성된 채팅방 id
    token: string; // 생성된 openvidu sessionId
    chatRoomId: string; // session 접속용 토큰
}

export interface ChatParticipant {
    userId: string;
    mbti: string;
    concern: string;
    planetId: number;
    energy: number;
}

export interface ParticipantsDataResponse extends BaseResponseType {
    data: {
        participants: ChatParticipant[];
        similarity: number;
    };
}

export interface ReconnectDataResponse extends BaseResponseType {
    data: {
        chatRoomId: string;
        sessionId: string;
        token: string;
    };
}

export interface Participant {
    userId: string;
    mbti: string;
    concern: string;
    planetId: number;
    energy: number;
}

export interface LetterFormValues {
    receiverId: string;
    content: string;
    chatRoomId: string;
}
