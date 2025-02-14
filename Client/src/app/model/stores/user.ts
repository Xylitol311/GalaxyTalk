import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
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

export const useUserStore = create<State & Actions>()(
    persist(
        (set, get) => ({
            ...initialState,
            setUserBase: (userInfo: UserBaseType) => {
                set((state) => ({
                    ...state,
                    ...userInfo,
                }));
            },
            setUserStatus: (userState: UserStatusType) => {
                set((state) => ({
                    ...state,
                    ...userState,
                }));
            },
            getUserBase: () => {
                const { userId, mbti, planetId, energy, role } = get();
                return { userId, mbti, planetId, energy, role };
            },
            reset: () => {
                set(initialState);
            },
        }),
        {
            name: 'user-storage',
            storage: createJSONStorage(() => localStorage),
        }
    )
);
