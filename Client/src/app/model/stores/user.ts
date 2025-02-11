import { create } from 'zustand';
import { UserBaseType, UserStatusType } from '../types/user';

type State = UserBaseType & UserStatusType;

type Actions = {
    setUserBase: (userInfo: UserBaseType) => void;
    setUserStatus: (userState: UserStatusType) => void;
    getUserBase: () => UserBaseType;
    reset: () => void;
};

const initialState: State = {
    userId: '',
    mbti: '',
    planetId: 0,
    energy: 0,
    role: null,
    UserInteractionState: 'idle',
};

export const useUserStore = create<State & Actions>()((set, get) => ({
    ...initialState,
    setUserBase: (userInfo: UserBaseType) => {
        const { userId, mbti, planetId, energy, role } = userInfo;
        set({ userId, mbti, planetId, energy, role });
    },
    setUserStatus: (userState: UserStatusType) => {
        set({ ...userState });
    },
    getUserBase: () => {
        const { userId, mbti, planetId, energy, role } = get();
        return { userId, mbti, planetId, energy, role };
    },
    reset: () => {
        set(initialState);
    },
}));
