import { ReactNode, useCallback, useEffect } from 'react';
import { useUserStore } from '@/app/model/stores/user';
import {
    useUserInfoQuery,
    useUserStatusQuery,
} from '@/features/user/api/queries';

export default function UserProvider({ children }: { children: ReactNode }) {
    // Memo: 에러 처리(fethcer에서), 로딩 처리(논의 필요)
    const { setUserBase, setUserStatus } = useUserStore();
    const { data: userBaseInfo, isSuccess: isInfoSuccess } = useUserInfoQuery();
    const { data: userStatus, isSuccess: isStatusSuccess } =
        useUserStatusQuery();

    const isQuerySuccess = isInfoSuccess && isStatusSuccess;

    const setUserInfo = useCallback(() => {
        if (!isQuerySuccess) {
            return;
        }

        setUserBase(userBaseInfo.data);
        setUserStatus(userStatus.data);
    }, [isQuerySuccess, setUserBase, setUserStatus, userBaseInfo, userStatus]);

    useEffect(() => {
        if (isQuerySuccess) {
            setUserInfo();
        }
    }, [isQuerySuccess, setUserInfo]);

    return <>{children}</>;
}
