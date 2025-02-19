import { ReactNode, useEffect } from 'react';
import { useUserStore } from '@/app/model/stores/user';
import {
    useUserInfoQuery,
    useUserStatusQuery,
} from '@/features/user/api/queries';

export default function UserProvider({ children }: { children: ReactNode }) {
    const { setUserBase, setUserStatus, userId } = useUserStore();

    const shouldFetch = !userId;

    const { data: userBaseInfo, isSuccess: isInfoSuccess } =
        useUserInfoQuery(shouldFetch);
    const { data: userStatus, isSuccess: isStatusSuccess } =
        useUserStatusQuery();

    useEffect(() => {
        if (isInfoSuccess) {
            setUserBase(userBaseInfo.data);
        }

        if (isStatusSuccess) {
            setUserStatus(userStatus.data);
            if (userStatus.data.userInteractionState !== 'chatting') {
                // userStatus가 chatting 상태가 아니라면, 로컬스토리지의 chatdata 삭제
                localStorage.removeItem('chatdata');
            }

            // UserInteractionState에 따른 네비게이션 처리
            // switch (userStatus.data.userInteractionState) {
            //     case 'matching':
            //         window.location.href = `${BASE_URL}${PATH.ROUTE.MATCH}`;
            //         break;
            //     case 'chatting':
            //         window.location.href = `${BASE_URL}${PATH.ROUTE.CHAT}`;
            //         break;
            //     case 'idle':
            //     default:
            //         // idle 상태 또는 예상치 못한 상태는 현재 페이지에 머뭅니다.
            //         break;
            // }
        }
    }, [
        setUserBase,
        setUserStatus,
        userBaseInfo,
        userStatus,
        isInfoSuccess,
        isStatusSuccess,
    ]);

    return <>{children}</>;
}
