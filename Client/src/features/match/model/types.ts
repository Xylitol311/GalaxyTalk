import { MBTI_TYPES } from '@/app/config/constants/mbti';

export type WaitingUserType = {
    userId: string;
    concern: string;
    mbti: typeof MBTI_TYPES | null;
    status: 'WAITING' | 'MATCH_SUCCESS' | 'MATCH_FAILED' | 'CHAT_CREATED';
    startTime: string;
};
