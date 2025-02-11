import { MBTI_TYPES } from '@/app/config/constants/mbti';

export type SignUpUserType = {
    mbti: typeof MBTI_TYPES | null;
    planetId: string;
};
