import { create } from 'zustand';
import { UserBaseType, UserStatusType } from '../types/user';

type State = UserBaseType & UserStatusType;

type Actions = {
    setUserBase: (userInfo: UserBaseType) => void;
    setUserStatus: (userState: UserStatusType) => void;
    reset: () => void;
};

const initialState: State = {
    userId: '',
    mbti: '',
    planetId: 0,
    energy: 0,
    role: 'GUEST',
    UserInteractionState: 'idle',
};

export const useUserStore = create<State & Actions>()((set) => ({
    ...initialState,
    setUserBase: (userInfo: UserBaseType) => {
        const { userId, mbti, planetId, energy, role } = userInfo;
        set({ userId, mbti, planetId, energy, role });
    },
    setUserStatus: (userState: UserStatusType) => {
        set({ ...userState });
    },
    reset: () => {
        set(initialState);
    },
}));
