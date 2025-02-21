export type UserBaseType = {
    userId: string;
    mbti: string;
    planetId: number;
    energy: number;
    role:
        | 'ROLE_ADMIN'
        | 'ROLE_USER'
        | 'ROLE_GUEST'
        | 'ROLE_WITHDRAW'
        | 'ROLE_RESTRICTED'
        | null;
};

export type UserStatusType = {
    userInteractionState: 'idle' | 'matching' | 'chatting';
};
