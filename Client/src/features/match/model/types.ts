export type WaitingUserType = {
    userId: string;
    concern: string;
    mbti:
        | 'ENFJ'
        | 'ENFP'
        | 'ENTJ'
        | 'ENTP'
        | 'ESFJ'
        | 'ESFP'
        | 'ESTJ'
        | 'ESTP'
        | 'INFJ'
        | 'INFP'
        | 'INTJ'
        | 'INTP'
        | 'ISFJ'
        | 'ISFP'
        | 'ISTJ'
        | 'ISTP'
        | null;
    status: 'WAITING' | 'MATCH_SUCCESS' | 'MATCH_FAILED' | 'CHAT_CREATED';
    startTime: string;
};
