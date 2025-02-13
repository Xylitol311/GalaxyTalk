// 기본이 되는 메시지 내용 인터페이스
export interface BaseMessage {
    content: string;
}
// 수신 및 조회용 메시지 인터페이스
export interface Message extends BaseMessage {
    senderId: string;
    createdAt: string;
}

export interface ChatRoom {
    chatRoomId: string;
    myConcern: string;
    participantConcern: string;
    participantPlanet: string;
    chatRoomCreatedAt: string;
    participantReview: string;
}

export interface AIQuestion {
    questionId: string;
    content: string;
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

export interface ParticipantsData {
    participants: ChatParticipant[];
    similarity: number;
}

export interface ReconnectData {
    chatRoomId: string;
    sessionId: number;
    token: string;
}
