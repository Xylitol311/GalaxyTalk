export type UserBaseType = {
    userId: string;
    mbti: string;
    planetId: number;
    energy: number;
    role: 'ADMIN' | 'USER' | 'GUEST' | 'WITHDRAW' | 'RESTRICTED' | null;
};

export type UserStatusType = {
    UserInteractionState: 'idle' | 'matching' | 'chatting';
};
