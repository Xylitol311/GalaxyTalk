export type UserBaseType = {
    userId: string;
    mbti: string;
    planetId: number;
    energy: number;
    role: 'ADMIN' | 'USER' | 'GUEST' | 'WITHDRAW' | 'RESTRICTED';
};

export type UserStatusType = {
    UserInteractionState: 'idle' | 'matching' | 'chatting';
};
