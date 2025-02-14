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
        useUserStatusQuery(shouldFetch);

    useEffect(() => {
        if (isInfoSuccess) {
            setUserBase(userBaseInfo.data);
        }

        if (isStatusSuccess) {
            setUserStatus(userStatus.data);
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
